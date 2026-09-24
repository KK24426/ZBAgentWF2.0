/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证项目创建、原子追加和等待异步结果的串行执行规则。
 */
package com.kk24426.zbagentwf.agent.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.kk24426.zbagentwf.agent.codex.*;
import com.kk24426.zbagentwf.common.agent.bean.*;
import com.kk24426.zbagentwf.common.project.bean.*;
import com.kk24426.zbagentwf.user.agent.userif.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectUserifImplTest {
    @TempDir Path temp;

    @Test
    void newProjectsUseUniqueDirectoriesAndPlanInitialRequirements() throws Exception {
        var planner = new CodexRequirementPlanner(CodexFixtureSupport.client("success"));
        var service = service(planner, new ManualExecutor());
        assertFalse(Files.exists(temp.resolve("projects")));
        Project first = service.newProject("第一项目");
        Project second = service.newProject("第二项目");
        assertNotEquals(first.getProjectId(), second.getProjectId());
        assertDoesNotThrow(() -> UUID.fromString(first.getProjectId()));
        assertEquals(temp.resolve("projects").resolve(first.getProjectId()), first.getWorkingDirectory());
        assertTrue(Files.isDirectory(first.getWorkingDirectory()));
        assertEquals(2, first.getRequirements().size());
        assertEquals("第一项目", first.getRequirements().getFirst().getUserContent());
        first.getRequirements().getFirst().getTasks().clear();
        assertEquals(2, second.getRequirements().getFirst().getTasks().size());
        var old = new ArrayList<>(first.getRequirements());
        var added = service.createRequirements(first, "追加内容");
        assertEquals(2, added.size());
        assertEquals(4, first.getRequirements().size());
        assertSame(old.getFirst(), first.getRequirements().getFirst());
        assertSame(added.getFirst(), first.getRequirements().get(2));
    }

    @Test
    void failedPlanningDoesNotAppendAndNewProjectCleansOnlyItsEmptyDirectory() throws Exception {
        var planner = mock(CodexRequirementPlanner.class);
        when(planner.plan(any(), anyString())).thenThrow(new IllegalStateException("fixture"));
        var service = service(planner, new ManualExecutor());
        assertThrows(IllegalStateException.class, () -> service.newProject("任务"));
        try (var directories = Files.list(temp.resolve("projects"))) { assertEquals(0, directories.count()); }
        Project project = existing();
        var original = project.getRequirements();
        assertThrows(IllegalStateException.class, () -> service.createRequirements(project, "任务"));
        assertSame(original, project.getRequirements());
        doAnswer(invocation -> {
            Files.writeString(((Path) invocation.getArgument(0)).resolve("user-file"), "用户数据");
            throw new IllegalStateException("fixture");
        }).when(planner).plan(any(), anyString());
        assertThrows(IllegalStateException.class, () -> service.newProject("保留"));
        try (var paths = Files.walk(temp.resolve("projects"))) {
            assertEquals(1, paths.filter(path -> path.getFileName().toString().equals("user-file")).count());
        }
    }

    @Test
    void executesInSelectionOrderWaitsAndStopsOnConfirmationThenRetriesWithNewExecutionId() throws Exception {
        Project project = existing();
        var first = requirement("a", "b");
        var second = requirement("c");
        project.getRequirements().addAll(List.of(first, second));
        var executor = new ManualExecutor();
        var service = service(mock(CodexRequirementPlanner.class), executor);
        try (var callers = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<List<Requirement>> call = callers.submit(() -> service.execTask(project, List.of(second, first)));
            Invocation c = executor.next();
            assertTrue(c.content().startsWith("c"));
            assertEquals(TaskStatus.RUNNING, second.getTasks().getFirst().getStatus());
            assertFalse(call.isDone());
            c.complete(true, false);
            Invocation a = executor.next();
            assertTrue(a.content().startsWith("a"));
            a.complete(false, true);
            assertEquals(List.of(second, first), call.get(5, TimeUnit.SECONDS));
            assertEquals(TaskStatus.NEEDS_CONFIRMATION, first.getTasks().getFirst().getStatus());
            assertEquals(TaskStatus.PENDING, first.getTasks().get(1).getStatus());
            assertTrue(executor.calls.isEmpty());
            assertEquals(List.of(second, first), service.execTask(project, List.of(second, first)));
            assertTrue(executor.calls.isEmpty(), "未确认不能跳过原 Task 继续执行");

            String planningId = first.getTasks().getFirst().getId();
            first.setUserConfirmMsg("同意");
            first.getTasks().getFirst().setStatus(TaskStatus.PENDING);
            Future<List<Requirement>> retry = callers.submit(() -> service.execTask(project, List.of(first)));
            Invocation repeated = executor.next();
            assertNotEquals(a.id(), repeated.id());
            assertTrue(repeated.memory().contains("同意"));
            assertTrue(repeated.memory().contains("请确认"));
            assertTrue(repeated.memory().contains("fixture summary"));
            assertTrue(repeated.memory().contains(planningId));
            assertTrue(repeated.memory().contains(a.id()));
            repeated.complete(true, false);
            Invocation b = executor.next();
            assertTrue(b.content().startsWith("b"));
            assertTrue(b.memory().contains("fixture summary"));
            b.complete(true, false);
            retry.get(5, TimeUnit.SECONDS);
            assertEquals(planningId, first.getTasks().getFirst().getId());
            assertEquals(repeated.id(), first.getTasks().getFirst().getResult().getTaskId());
            assertEquals(TaskStatus.SUCCEEDED, first.getTasks().get(1).getStatus());
        }
    }

    @Test
    void ordinaryFailureStopsAndRejectionDoesNotInventAResult() throws Exception {
        Project project = existing();
        var requirement = requirement("a", "b");
        project.getRequirements().add(requirement);
        var executor = new ManualExecutor();
        var service = service(mock(CodexRequirementPlanner.class), executor);
        executor.reject = true;
        assertThrows(RejectedExecutionException.class, () -> service.execTask(project, List.of(requirement)));
        assertEquals(TaskStatus.PENDING, requirement.getTasks().getFirst().getStatus());
        assertNull(requirement.getTasks().getFirst().getResult());
        executor.reject = false;
        try (var callers = Executors.newVirtualThreadPerTaskExecutor()) {
            var call = callers.submit(() -> service.execTask(project, List.of(requirement)));
            executor.next().complete(false, false);
            call.get(5, TimeUnit.SECONDS);
            assertEquals(TaskStatus.FAILED, requirement.getTasks().getFirst().getStatus());
            assertEquals(TaskStatus.PENDING, requirement.getTasks().get(1).getStatus());
            assertTrue(executor.calls.isEmpty());
            assertEquals(List.of(requirement), service.execTask(project, List.of(requirement)));
            assertTrue(executor.calls.isEmpty(), "未重置失败 Task 不能继续执行");
            requirement.getTasks().getFirst().setStatus(TaskStatus.PENDING);
            var retry = callers.submit(() -> service.execTask(project, List.of(requirement)));
            Invocation next = executor.next();
            assertTrue(next.memory().contains("fixture failure"));
            next.complete(true, false);
            executor.next().complete(true, false);
            retry.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void rejectsForeignRequirementsDuplicateTasksAndOutsideDirectoriesBeforeSubmitting() throws Exception {
        Project project = existing();
        var requirement = requirement("a");
        project.getRequirements().add(requirement);
        var executor = new ManualExecutor();
        var service = service(mock(CodexRequirementPlanner.class), executor);
        assertThrows(IllegalArgumentException.class, () -> service.execTask(project, List.of(requirement("foreign"))));
        assertThrows(IllegalArgumentException.class, () -> service.execTask(project, List.of(requirement, requirement)));
        var duplicateOwner = new Requirement();
        duplicateOwner.getTasks().add(requirement.getTasks().getFirst());
        project.getRequirements().add(duplicateOwner);
        assertThrows(IllegalArgumentException.class, () -> service.execTask(project, List.of(requirement)));
        project.setWorkingDirectory(Files.createDirectory(temp.resolve("outside")));
        assertThrows(IllegalArgumentException.class, () -> service.execTask(project, List.of(requirement)));
        assertTrue(executor.calls.isEmpty());
    }

    @Test
    void interruptionWaitsForAcceptedResultAndDoesNotStartNextTask() throws Exception {
        Project project = existing();
        var requirement = requirement("a", "b");
        project.getRequirements().add(requirement);
        var executor = new ManualExecutor();
        var service = service(mock(CodexRequirementPlanner.class), executor);
        var finished = new CompletableFuture<Boolean>();
        Thread caller = Thread.ofVirtual().start(() -> {
            service.execTask(project, List.of(requirement));
            finished.complete(Thread.currentThread().isInterrupted());
        });
        Invocation invocation = executor.next();
        caller.interrupt();
        assertFalse(finished.isDone());
        invocation.complete(true, false);
        assertTrue(finished.get(5, TimeUnit.SECONDS));
        assertEquals(TaskStatus.SUCCEEDED, requirement.getTasks().getFirst().getStatus());
        assertEquals(TaskStatus.PENDING, requirement.getTasks().get(1).getStatus());
        assertTrue(executor.calls.isEmpty());
    }

    private ProjectUserifImpl service(CodexRequirementPlanner planner, AgentExec executor) {
        return new ProjectUserifImpl(new ProjectSettings(temp.resolve("projects")), planner, executor);
    }

    private Project existing() throws Exception {
        var project = new Project();
        project.setProjectId(UUID.randomUUID().toString());
        project.setWorkingDirectory(Files.createDirectories(temp.resolve("projects").resolve(project.getProjectId())));
        return project;
    }

    private Requirement requirement(String... contents) {
        var requirement = new Requirement();
        for (String content : contents) {
            var task = new RequirementTask();
            task.setId(UUID.randomUUID().toString()); task.setContent(content); task.setAcceptanceCriteria("验收");
            requirement.getTasks().add(task);
        }
        return requirement;
    }

    private static final class ManualExecutor extends AgentExec {
        final BlockingQueue<Invocation> calls = new LinkedBlockingQueue<>();
        boolean reject;
        ManualExecutor() { super(new AgentBean()); }
        @Override public String exec(Project project, String content, String memory, AgentExecCallback callback) {
            if (reject) throw new RejectedExecutionException("fixture");
            String id = UUID.randomUUID().toString();
            calls.add(new Invocation(id, content, memory, callback));
            return id;
        }
        @Override public String getStderr(String taskId) { return null; }
        Invocation next() throws Exception { return Objects.requireNonNull(calls.poll(5, TimeUnit.SECONDS), "没有收到执行"); }
    }

    private record Invocation(String id, String content, String memory, AgentExecCallback callback) {
        void complete(boolean success, boolean confirmation) {
            var result = new AgentExecResult(); result.setTaskId(id); result.setSuccess(success);
            result.setConfirmationRequired(confirmation); result.setSummary("fixture summary");
            if (confirmation) result.setConfirmationMessage("请确认");
            if (!success && !confirmation) result.setErrorMessage("fixture failure");
            callback.onCompleted(result);
        }
    }
}
