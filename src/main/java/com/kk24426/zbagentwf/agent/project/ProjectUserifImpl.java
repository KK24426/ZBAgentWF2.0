/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：实现 UUID 项目创建、需求追加及遇到失败或确认即停止的串行 Task 执行。
 */
package com.kk24426.zbagentwf.agent.project;

import com.kk24426.zbagentwf.agent.codex.CodexRequirementPlanner;
import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;
import com.kk24426.zbagentwf.common.project.bean.*;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecutor;
import com.kk24426.zbagentwf.user.project.userif.ProjectUserif;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

/** 显式注入已选择的执行器和规划器；模型发现及 Spring 自动装配另行实现。 */
public final class ProjectUserifImpl extends ProjectUserif {
    private final ProjectSettings settings;
    private final CodexRequirementPlanner planner;
    private final AgentExecutor executor;
    private final Map<Path, Object> locks = new ConcurrentHashMap<>();

    public ProjectUserifImpl(ProjectSettings settings, CodexRequirementPlanner planner, AgentExecutor executor) {
        this.settings = Objects.requireNonNull(settings);
        this.planner = Objects.requireNonNull(planner);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public Project newProject(String content) {
        requireContent(content);
        Path created = null;
        try {
            Path root = settings.getRootDirectory();
            Files.createDirectories(root);
            var project = new Project();
            project.setProjectId(UUID.randomUUID().toString());
            created = Files.createDirectory(root.resolve(project.getProjectId()));
            project.setWorkingDirectory(created.toAbsolutePath().normalize());
            createRequirements(project, content);
            return project;
        } catch (IOException | RuntimeException failure) {
            if (created != null) {
                // 只删除本次创建的空目录；若有外部写入则保留，不递归删除任何内容。
                try { Files.delete(created); }
                catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            }
            throw new IllegalStateException("项目创建失败。", failure);
        }
    }

    @Override
    public List<Requirement> createRequirements(Project project, String content) {
        requireContent(content);
        Path directory = directory(project);
        synchronized (locks.computeIfAbsent(directory, ignored -> new Object())) {
            List<Requirement> existing = Objects.requireNonNull(project.getRequirements(), "项目需求列表不能为空。");
            List<Requirement> added = planner.plan(directory, content);
            // 用新的可修改列表整体替换，兼容 Bean setter 接收的不可变列表，避免部分追加。
            var combined = new ArrayList<>(existing);
            combined.addAll(added);
            project.setRequirements(combined);
            return new ArrayList<>(added);
        }
    }

    @Override
    public List<Requirement> execTask(Project project, List<Requirement> requirements) {
        Path directory = directory(project);
        synchronized (locks.computeIfAbsent(directory, ignored -> new Object())) {
            List<Requirement> selected = validateSelection(project, requirements);
            boolean interrupted = false;
            try {
                for (Requirement requirement : selected) {
                    for (RequirementTask task : requirement.getTasks()) {
                        if (Thread.currentThread().isInterrupted()) return selected;
                        if (task.getStatus() == TaskStatus.FAILED || task.getStatus() == TaskStatus.NEEDS_CONFIRMATION) return selected;
                        if (task.getStatus() != TaskStatus.PENDING) continue;
                        var completion = new CompletableFuture<AgentExecResult>();
                        AgentExecResult previous = task.getResult();
                        // 重试是新会话；先保存原确认问题/失败原因和新答复，再清除当前结果。
                        String context = memory(requirement);
                        task.setResult(null);
                        task.setStatus(TaskStatus.RUNNING);
                        String id;
                        try {
                            id = executor.exec(project, task.getContent() + "\n验收标准：\n"
                                    + Objects.toString(task.getAcceptanceCriteria(), ""), context, completion::complete);
                        } catch (RejectedExecutionException failure) {
                            task.setStatus(TaskStatus.PENDING);
                            task.setResult(previous);
                            throw failure;
                        }
                        AgentExecResult result;
                        while (true) {
                            try { result = completion.get(); break; }
                            catch (InterruptedException failure) { interrupted = true; }
                            catch (ExecutionException failure) { throw new IllegalStateException("等待执行结果失败。", failure); }
                        }
                        if (result == null || !Objects.equals(id, result.getTaskId())
                                || result.isSuccess() && result.isConfirmationRequired()) {
                            result = new AgentExecResult();
                            result.setTaskId(id);
                            result.setErrorMessage("底层执行结果违反回调契约。");
                        }
                        task.setResult(result);
                        task.setStatus(result.isConfirmationRequired() ? TaskStatus.NEEDS_CONFIRMATION
                                : result.isSuccess() ? TaskStatus.SUCCEEDED : TaskStatus.FAILED);
                        if (interrupted || task.getStatus() != TaskStatus.SUCCEEDED) return selected;
                    }
                }
                return selected;
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }

    private Path directory(Project project) {
        if (project == null || project.getProjectId() == null || project.getProjectId().isBlank()
                || project.getWorkingDirectory() == null) throw new IllegalArgumentException("项目缺少标识或目录。");
        try {
            Path root = settings.getRootDirectory().toRealPath();
            Path actual = project.getWorkingDirectory().toRealPath();
            if (!Files.isDirectory(actual) || actual.equals(root) || !actual.startsWith(root)) {
                throw new IllegalArgumentException("项目目录必须位于配置的根目录之下。");
            }
            return actual;
        } catch (IOException failure) {
            throw new IllegalArgumentException("项目目录不可用。", failure);
        }
    }

    private static List<Requirement> validateSelection(Project project, List<Requirement> requirements) {
        if (requirements == null || project.getRequirements() == null) throw new IllegalArgumentException("需求列表不能为空。");
        Set<Requirement> owned = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<RequirementTask> tasks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Requirement requirement : project.getRequirements()) {
            if (requirement == null || !owned.add(requirement) || requirement.getTasks() == null) {
                throw new IllegalArgumentException("项目包含无效或重复的需求。");
            }
            for (RequirementTask task : requirement.getTasks()) {
                if (task == null || !tasks.add(task)) throw new IllegalArgumentException("Task 不能被重复归属。");
            }
        }
        Set<Requirement> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var selected = new ArrayList<Requirement>();
        for (Requirement requirement : requirements) {
            if (!owned.contains(requirement) || !seen.add(requirement)) throw new IllegalArgumentException("执行范围不属于该项目或存在重复。");
            Set<String> ids = new HashSet<>();
            for (RequirementTask task : requirement.getTasks()) {
                if (task.getStatus() == null || task.getId() == null || task.getId().isBlank()
                        || !ids.add(task.getId()) || task.getContent() == null || task.getContent().isBlank()) {
                    throw new IllegalArgumentException("Task 缺少有效状态、唯一标识或内容。");
                }
            }
            selected.add(requirement);
        }
        return selected;
    }

    private static String memory(Requirement requirement) {
        var memory = new StringBuilder();
        memory.append("用户原始需求：\n").append(Objects.toString(requirement.getUserContent(), ""))
                .append("\n需求理解：\n").append(Objects.toString(requirement.getAgentUnderstanding(), ""))
                .append("\n需求验收：\n").append(Objects.toString(requirement.getAcceptanceCriteria(), ""))
                .append("\n确认相关内容：\n").append(Objects.toString(requirement.getUserConfirmMsg(), ""));
        for (RequirementTask task : requirement.getTasks()) {
            AgentExecResult previous = task.getResult();
            if (previous != null) {
                memory.append("\n规划任务：").append(task.getId())
                        .append("\n先前执行：").append(Objects.toString(previous.getTaskId(), ""))
                        .append("\n先前结果：\n").append(Objects.toString(previous.getSummary(), ""))
                        .append("\n先前待确认问题：\n").append(Objects.toString(previous.getConfirmationMessage(), ""))
                        .append("\n先前失败原因：\n").append(Objects.toString(previous.getErrorMessage(), ""));
            }
        }
        return memory.toString();
    }

    private static void requireContent(String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("用户内容不能为空。");
    }
}
