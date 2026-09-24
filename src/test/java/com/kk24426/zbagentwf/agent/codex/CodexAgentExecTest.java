/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：使用真实本地子进程验证异步执行、协议、回调及关闭资源。
 */
package com.kk24426.zbagentwf.agent.codex;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.agent.bean.*;
import com.kk24426.zbagentwf.common.project.bean.Project;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexAgentExecTest {
    @TempDir Path temp;

    @Test
    void submitsWithCwdStdinAndStructuredArgumentsThenCallsOnce() throws Exception {
        Project project = project("中文 project");
        var count = new AtomicInteger();
        try (var executor = executor("success")) {
            var done = new CompletableFuture<AgentExecResult>();
            String id = executor.exec(project, "中文任务 ' ; $(literal)", "先前记忆", result -> {
                count.incrementAndGet(); done.complete(result);
            });
            AgentExecResult result = done.get(20, TimeUnit.SECONDS);
            assertEquals(id, result.getTaskId());
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertFalse(result.isConfirmationRequired());
            assertEquals(18L, result.getTokenCount());
            assertTrue(result.getSummary().contains(project.getWorkingDirectory().toRealPath().toString()));
            assertTrue(result.getSummary().contains("中文任务 ' ; $(literal)"));
            assertTrue(result.getSummary().contains("先前记忆"));
            assertTrue(result.getSummary().contains("workspace-write"));
            assertTrue(result.getSummary().contains("fixture-model ; $(literal)"));
            assertFalse(executor.getStderr(id).contains("fixture-secret"));
            assertTrue(executor.getStderr(id).contains("REDACTED"));
            assertNull(executor.getStderr("unknown"));
            assertFalse(Files.exists(Path.of(Files.readString(project.getWorkingDirectory().resolve("schema-path")))));
            assertEquals(1, count.get());
        }
    }

    @Test
    void failureConfirmationAndUnknownTokensUseDistinctOutcomes() throws Exception {
        for (String scenario : List.of("failure", "confirmation", "unknown-tokens")) {
            try (var executor = executor(scenario)) {
                var result = submit(executor, project(scenario));
                assertEquals(scenario.equals("unknown-tokens"), result.isSuccess());
                assertEquals(scenario.equals("confirmation"), result.isConfirmationRequired());
                if (scenario.equals("confirmation")) assertEquals("请确认执行范围", result.getConfirmationMessage());
                if (scenario.equals("unknown-tokens")) assertNull(result.getTokenCount());
            }
        }
    }

    @Test
    void rejectsBadTransportAndContradictoryFinalResults() throws Exception {
        for (String scenario : List.of("bad-json", "contradiction", "exit-failure", "event-error",
                "duplicate-completion", "missing-completion", "output-limit")) {
            try (var executor = executor(scenario)) {
                AgentExecResult result = submit(executor, project(scenario));
                assertFalse(result.isSuccess(), scenario);
                assertFalse(result.isConfirmationRequired(), scenario);
                assertNotNull(result.getErrorMessage());
                assertFalse(result.getErrorMessage().contains("private"));
            }
        }
    }

    @Test
    void drainsLargeStderrAndInputWithoutDeadlock() throws Exception {
        try (var executor = executor("flood")) {
            var done = new CompletableFuture<AgentExecResult>();
            String id = executor.exec(project("flood"), "中文".repeat(100000), "", done::complete);
            assertTrue(done.get(20, TimeUnit.SECONDS).isSuccess());
            assertTrue(executor.getStderr(id).contains("诊断已截断"));
            assertTrue(executor.getStderr(id).length() < 66000);
        }
    }

    @Test
    void timeoutKillsProcessAndCleansSchemaEvenWhenStdinIsNotConsumed() throws Exception {
        Project project = project("timeout");
        try (var executor = new CodexAgentExec(new AgentBean(), CodexFixtureSupport.client("no-input", Duration.ofSeconds(2)))) {
            var done = new CompletableFuture<AgentExecResult>();
            executor.exec(project, "x".repeat(1024 * 1024), "", done::complete);
            assertFalse(done.get(10, TimeUnit.SECONDS).isSuccess());
            long pid = Long.parseLong(Files.readString(project.getWorkingDirectory().resolve("started")));
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
            assertFalse(Files.exists(Path.of(Files.readString(project.getWorkingDirectory().resolve("schema-path")))));
        }
    }

    @Test
    void closesObservedDescendantThatKeepsPipesOpenAfterParentExit() throws Exception {
        Project project = project("descendant");
        try (var executor = executor("descendant")) {
            var done = new CompletableFuture<AgentExecResult>();
            executor.exec(project, "task", null, done::complete);
            assertNotNull(done.get(10, TimeUnit.SECONDS));
            long pid = Long.parseLong(Files.readString(project.getWorkingDirectory().resolve("child-pid")));
            ProcessHandle child = ProcessHandle.of(pid).orElse(null);
            if (child != null) child.onExit().get(5, TimeUnit.SECONDS);
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        }
    }

    @Test
    void closeReleasesCacheAndCompletesAcceptedWorkWhileRejectingFurtherSubmissions() throws Exception {
        var executor = executor("sleep");
        var completions = new ArrayList<CompletableFuture<AgentExecResult>>();
        var ids = new ArrayList<String>();
        var count = new AtomicInteger();
        try {
            for (int i = 0; i < 4; i++) {
                var done = new CompletableFuture<AgentExecResult>();
                completions.add(done);
                ids.add(executor.exec(project("close" + i), "task", null, result -> { count.incrementAndGet(); done.complete(result); }));
            }
            assertThrows(RejectedExecutionException.class, () -> executor.exec(project("full"), "task", null, ignored -> fail()));
            executor.close();
            for (var done : completions) assertFalse(done.get(10, TimeUnit.SECONDS).isSuccess());
            assertEquals(4, count.get());
            for (String id : ids) assertNull(executor.getStderr(id));
            assertThrows(RejectedExecutionException.class, () -> executor.exec(project("closed"), "task", null, ignored -> fail()));
        } finally { executor.close(); }
    }

    @Test
    void invalidSubmissionDoesNotCallbackAndCallbackMayCloseOrThrow() throws Exception {
        var executor = executor("success");
        try {
            assertThrows(RejectedExecutionException.class, () -> executor.exec(null, "task", null, ignored -> fail()));
            var count = new AtomicInteger();
            var finished = new CompletableFuture<Void>();
            executor.exec(project("reentrant"), "task", null, result -> {
                count.incrementAndGet(); executor.close(); finished.complete(null); throw new IllegalStateException("private callback input");
            });
            finished.get(20, TimeUnit.SECONDS);
            assertEquals(1, count.get());
        } finally { executor.close(); }
    }

    private CodexAgentExec executor(String scenario) { return new CodexAgentExec(new AgentBean(), CodexFixtureSupport.client(scenario)); }

    private AgentExecResult submit(CodexAgentExec executor, Project project) throws Exception {
        var done = new CompletableFuture<AgentExecResult>();
        String id = executor.exec(project, "任务", null, done::complete);
        var result = done.get(20, TimeUnit.SECONDS);
        assertEquals(id, result.getTaskId());
        return result;
    }

    private Project project(String name) throws Exception {
        var project = new Project();
        project.setProjectId(name);
        project.setWorkingDirectory(Files.createDirectory(temp.resolve(name)));
        return project;
    }
}
