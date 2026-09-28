/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：验证受理回调、任务身份、安全拒绝、协议故障和资源收尾；模型使用Java子进程替身。
 */
package com.kk24426.zbagentwf.agent.codex;
import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.registry.ProjectFixtureSupport;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.user.agent.api.ProjectAgentExecutor;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证受理回调、任务身份、安全拒绝、协议故障和资源收尾；模型使用Java子进程替身。 */
class CodexAgentExecutorTest {
    @TempDir Path temp;
    /** 成功单任务必须只回调一次，审核输入等于执行输入，业务主键和执行UUID分别保留。 */
    @Test void structuredCallAndSingleCompletionPreserveTaskAndExecutionIdentity() throws Exception {
        try(var env=environment("success")) {
            var p=env.single();var t=p.getRequirements().getFirst().getTasks().getFirst();var executor=env.factory.getExecutor(p);
            var done=new CompletableFuture<AgentExecutionResult>();var count=new AtomicInteger();
            String id=executor.execTasks(t.getId(),r -> {count.incrementAndGet();done.complete(r);});
            var result=done.get(20,TimeUnit.SECONDS);
            assertTrue(result.isSuccess(),result.getErrorMessage());assertEquals(id,result.getTaskId());assertEquals(1,count.get());
            assertEquals(36L,result.getTokenCount());assertEquals(TaskStatus.SUCCEEDED,t.getStatus());assertEquals(id,t.getResult().getTaskId());
            assertTrue(result.getSummary().contains("中文任务 ' ; $(literal)"));assertTrue(result.getSummary().contains("workspace-write"));
            Path dir=env.settings.directory(p.getProjectId());
            String audit=Files.readString(dir.resolve("audit-input.txt"));
            var payload=CodexJson.JSON.readTree(audit.substring(audit.indexOf("{\"payload\"")));
            assertEquals(payload.get("payload").asString(),Files.readString(dir.resolve("prompt-input.txt")));
            assertEquals(1,Files.readAllLines(dir.resolve("audit-count")).size());
            assertFalse(executor.getStderr(id).contains("fixture-secret"));assertNull(executor.getStderr("unknown"));
            assertFalse(Files.exists(Path.of(Files.readString(dir.resolve("schema-path")))));
        }
    }
    /** 覆盖失败、确认、未知用量及协议错误，不能把传输正常等同业务成功。 */
    @Test void failureConfirmationUnknownTokensAndTransportFailuresRemainDistinct() throws Exception {
        for(String scenario:List.of("failure","confirmation","unknown-tokens","bad-json","contradiction","exit-failure","event-error","duplicate-completion","missing-completion","output-limit")) {
            try(var env=environment(scenario)) {
                var p=env.single();var result=submit(env.factory.getExecutor(p),p);
                assertEquals(scenario.equals("unknown-tokens"),result.isSuccess(),scenario);
                assertEquals(scenario.equals("confirmation"),result.isConfirmationRequired(),scenario);
                if(scenario.equals("unknown-tokens"))assertNull(result.getTokenCount());
                if(!result.isSuccess() && !result.isConfirmationRequired())assertNotNull(result.getErrorMessage());
            }
        }
    }
    /** 审核拒绝、非法字段、故障和超时都必须在业务进程标记产生前停止。 */
    @Test void reviewRefusalMalformedVerdictAndTimeoutNeverStartBusinessProcess() throws Exception {
        for(String scenario:List.of("audit-deny","audit-bad","audit-field","audit-empty","audit-exit","audit-sleep")) {
            try(var env=environment(scenario)) {
                var p=env.single();assertFalse(submit(env.factory.getExecutor(p),p).isSuccess(),scenario);
                assertFalse(Files.exists(env.settings.path(p.getProjectId()).resolve("business-count")),scenario);
                assertEquals(TaskStatus.FAILED,p.getRequirements().getFirst().getTasks().getFirst().getStatus());
            }
        }
    }
    /** 大输入与双输出管道同时完成，聚合诊断按UTF-8字节限长。 */
    @Test void largeInputAndDiagnosticAreBoundedAndDrained() throws Exception {
        try(var env=environment("flood")) {
            var p=env.single();p.getRequirements().getFirst().getTasks().getFirst().setContent("中文".repeat(100000));
            var executor=env.factory.getExecutor(p);var result=submit(executor,p);assertTrue(result.isSuccess());
            assertTrue(executor.getStderr(result.getTaskId()).contains("诊断已截断"));
            assertTrue(executor.getStderr(result.getTaskId()).getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=65536);
        }
    }
    /** 子进程不读输入或后代继承管道时，超时仍回收已观察进程和临时schema。 */
    @Test void timeoutAndDescendantCleanupReleaseProcessesAndSchema() throws Exception {
        for(String scenario:List.of("no-input","descendant"))try(var env=environment(scenario)) {
            var p=env.single();submit(env.factory.getExecutor(p),p);Path dir=env.settings.path(p.getProjectId());
            long pid=Long.parseLong(Files.readString(dir.resolve(scenario.equals("descendant")?"child-pid":"started")));
            var handle=ProcessHandle.of(pid);if(handle.isPresent())handle.get().onExit().get(5,TimeUnit.SECONDS);
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
            assertFalse(Files.exists(Path.of(Files.readString(dir.resolve("schema-path")))));
        }
    }
    /** 四项目共享额度满后同步拒绝；关闭已受理任务仍最终回调且不受理新工作。 */
    @Test void sharedCapacityShutdownAndInvalidTaskDoNotCreateExtraCallbacks() throws Exception {
        try(var env=environment("sleep")) {
            var done=new ArrayList<CompletableFuture<AgentExecutionResult>>();
            for(int i=0;i<4;i++) {var p=env.single();var future=new CompletableFuture<AgentExecutionResult>();done.add(future);env.factory.getExecutor(p).execAllTasks(future::complete);}
            var p=env.single();var executor=env.factory.getExecutor(p);
            assertThrows(RejectedExecutionException.class,() -> executor.execAllTasks(r -> fail()));
            assertThrows(RejectedExecutionException.class,() -> executor.execTasks(9999L,r -> fail()));
            env.factory.close();for(var future:done)assertFalse(future.get(15,TimeUnit.SECONDS).isSuccess());
            assertThrows(IllegalStateException.class,() -> env.factory.getExecutor(p));
        }
    }
    /** 回调可重入关闭并抛异常，资源先归还且不产生第二次完成事件。 */
    @Test void callbackCanCloseAndThrowWithoutSecondCompletion() throws Exception {
        try(var env=environment("success")) {
            var p=env.single();var count=new AtomicInteger();var done=new CompletableFuture<Void>();
            env.factory.getExecutor(p).execAllTasks(r -> {count.incrementAndGet();env.factory.close();done.complete(null);throw new IllegalStateException("private");});
            done.get(20,TimeUnit.SECONDS);assertEquals(1,count.get());
        }
    }
    /** 已受理批次冻结规则；项目Bean后来追加的内容不能混入正在执行的最终输入。 */
    @Test void acceptedRulesRemainFrozenAndProjectIdentityCannotMove() throws Exception {
        try(var env=environment("success")) {
            var p=env.single(); p.setProjectPrompt(new Prompt("原规则")); var executor=env.factory.getExecutor(p);
            var done=new CompletableFuture<AgentExecutionResult>();
            synchronized(p) {
                executor.execAllTasks(done::complete);
                p.setProjectPrompt(new Prompt("后加规则"));
            }
            assertTrue(done.get(20,TimeUnit.SECONDS).isSuccess());
            String sent=Files.readString(env.settings.path(p.getProjectId()).resolve("prompt-input.txt"));
            assertTrue(sent.contains("原规则")); assertFalse(sent.contains("后加规则"));
            p.setProjectId(UUID.randomUUID().toString());
            assertThrows(IllegalArgumentException.class,()->env.factory.getExecutor(p));
            assertThrows(RejectedExecutionException.class,()->executor.execAllTasks(r->fail()));
        }
    }
    /** 每个场景独立目录与工厂，避免诊断标记和资源相互污染。 */
    private ProjectFixtureSupport environment(String scenario) {return new ProjectFixtureSupport(temp.resolve(UUID.randomUUID().toString()),"success",scenario);}
    /** 同步等待单任务最终回调并核对受理UUID，超出测试预算明确失败。 */
    private AgentExecutionResult submit(ProjectAgentExecutor executor,Project p) throws Exception {
        var done=new CompletableFuture<AgentExecutionResult>();String id=executor.execTasks(p.getRequirements().getFirst().getTasks().getFirst().getId(),done::complete);
        var result=done.get(20,TimeUnit.SECONDS);assertEquals(id,result.getTaskId());return result;
    }
}
