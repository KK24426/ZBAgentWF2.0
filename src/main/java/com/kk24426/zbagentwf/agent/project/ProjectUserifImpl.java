/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：实现 UUID 项目创建、需求追加及遇到失败或确认即停止的串行 Task 执行。
 */
package com.kk24426.zbagentwf.agent.project;

import com.kk24426.zbagentwf.agent.registry.AgentRequirementPlanner;
import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;
import com.kk24426.zbagentwf.common.project.bean.*;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecutor;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecFactory;
import com.kk24426.zbagentwf.user.project.userif.ProjectUserif;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

/** 创建时绑定三角色，后续操作读取项目中的绑定；实例本身不保存当前项目或默认模型。 */
public final class ProjectUserifImpl extends ProjectUserif {
    private final ProjectSettings settings;
    private final AgentRequirementPlanner planner;
    private final AgentExecFactory factory;
    private final Map<Path, ProjectLock> locks = new HashMap<>();

    public ProjectUserifImpl(ProjectSettings settings, AgentExecFactory factory, AgentRequirementPlanner planner) {
        this.settings = Objects.requireNonNull(settings);
        this.planner = Objects.requireNonNull(planner);
        this.factory = Objects.requireNonNull(factory);
    }

    @Override
    public Project newProject(String content, AgentBean planningAgent, AgentBean developmentAgent, AgentBean reviewAgent) {
        requireContent(content);
        // 三个模型全部解析成功后才产生项目目录等外部副作用。
        AgentExecutor planning = Objects.requireNonNull(factory.getExecutor(planningAgent));
        AgentExecutor development = Objects.requireNonNull(factory.getExecutor(developmentAgent));
        AgentExecutor review = Objects.requireNonNull(factory.getExecutor(reviewAgent));
        Path created = null;
        try {
            Path root = settings.getRootDirectory();
            Files.createDirectories(root);
            var project = new Project();
            project.setPlanningAgent(planning);
            project.setDevelopmentAgent(development);
            project.setReviewAgent(review);
            project.setProjectId(UUID.randomUUID().toString());
            // 只有业务创建调用才建立目录；createDirectory 要求项目目录全新，不能复用已有目录。
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
        try (var ignored = lock(directory)) {
            // 锁覆盖规划到列表替换的整个过程，避免同目录并发追加时各自覆盖对方的新增需求。
            List<Requirement> existing = Objects.requireNonNull(project.getRequirements(), "项目需求列表不能为空。");
            List<Requirement> added = planner.plan(project, content);
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
        try (var ignored = lock(directory)) {
            // 本批固定使用开始时绑定的开发执行器；审核角色不会在任务执行后被隐式调用。
            AgentExecutor executor = project.getDevelopmentAgent();
            if (executor == null) throw new IllegalArgumentException("项目未绑定开发 Agent。");
            List<Requirement> selected = validateSelection(project, requirements);
            boolean interrupted = false;
            try {
                for (Requirement requirement : selected) {
                    for (RequirementTask task : requirement.getTasks()) {
                        if (Thread.currentThread().isInterrupted()) return selected;
                        // 既有失败或待确认也会挡住后续任务；只有调用方显式重置 PENDING 才算重试。
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
                            // 未受理不会回调；恢复提交前的结果，避免 Task 永远停留在 RUNNING。
                            task.setStatus(TaskStatus.PENDING);
                            task.setResult(previous);
                            throw failure;
                        }
                        AgentExecResult result;
                        // 当前契约没有取消操作。调用方中断后仍须等已受理执行结束并落下结果，
                        // 随后停止本批并恢复中断标志，不能留下后台修改与前台状态脱节的 Task。
                        while (true) {
                            try { result = completion.get(); break; }
                            catch (InterruptedException failure) { interrupted = true; }
                            catch (ExecutionException failure) { throw new IllegalStateException("等待执行结果失败。", failure); }
                        }
                        // 回调可能来自其它实现或替身；标识不匹配、互斥状态冲突时统一记为失败。
                        if (result == null || !Objects.equals(id, result.getTaskId())
                                || result.isSuccess() && result.isConfirmationRequired()) {
                            result = new AgentExecResult();
                            result.setTaskId(id);
                            result.setErrorMessage("底层执行结果违反回调契约。");
                        }
                        task.setResult(result);
                        // 待确认也是本次执行的终态，等待用户补充后重新提交，不在这里暂停底层会话。
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
            // 先解析真实路径再比较归属，避免符号链接把表面位于根目录内的项目导向外部。
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

    private ProjectLock lock(Path directory) {
        ProjectLock value;
        synchronized (locks) {
            value = locks.computeIfAbsent(directory, ProjectLock::new);
            value.users++; // 等待者也持有引用，不能在解锁瞬间让同目录出现两把锁。
        }
        // 不持有 locks 映射锁等待项目锁，允许不同项目独立运行，也让释放者能归还引用。
        value.lock.lock();
        return value;
    }

    private final class ProjectLock implements AutoCloseable {
        private final Path directory;
        private final ReentrantLock lock = new ReentrantLock();
        private int users;
        private ProjectLock(Path directory) { this.directory = directory; }
        @Override public void close() {
            lock.unlock();
            // 解锁与减引用之间进入的新调用者也会先加引用，因此不会出现同目录两把有效锁。
            synchronized (locks) { if (--users == 0) locks.remove(directory, this); }
        }
    }

    private static List<Requirement> validateSelection(Project project, List<Requirement> requirements) {
        if (requirements == null || project.getRequirements() == null) throw new IllegalArgumentException("需求列表不能为空。");
        // 归属按实际对象身份判断；同内容的外部对象不属于项目，同一 Task 对象也不能被两条需求共享。
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
        // 先完整校验本次范围再执行，避免处理到中途才发现后面的需求不属于该项目。
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

    /** 新执行不复用旧 CLI 会话；用本需求的历史结果重建上下文，并保留确认答复对应的旧问题。 */
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
