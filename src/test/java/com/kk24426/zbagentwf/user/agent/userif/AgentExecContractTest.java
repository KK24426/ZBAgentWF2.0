/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：用手动完成的测试替身验证执行接口、项目传递和结果关联。
 */
package com.kk24426.zbagentwf.user.agent.userif;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;
import com.kk24426.zbagentwf.common.project.bean.Project;
import com.kk24426.zbagentwf.common.project.bean.Requirement;
import com.kk24426.zbagentwf.user.UserInterface;
import com.kk24426.zbagentwf.user.project.userif.ProjectUserif;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class AgentExecContractTest {
    @Test
    void callerAssociatesEachFinalResultAndDiagnosticsWithTheSubmittedProject() {
        var model = new AgentBean();
        var executor = new ManualExecutor(model);
        var first = project("project-1");
        var second = project("project-2");
        var results = new ArrayList<AgentExecResult>();
        String firstId = executor.exec(first, "  原始内容  ", null, results::add);
        String secondId = executor.exec(second, "另一任务", "先前记忆", results::add);
        assertTrue(results.isEmpty());
        assertNotEquals(firstId, secondId);
        assertSame(model, executor.getAgent());
        assertSame(first, executor.pending.get(firstId).project());
        assertSame(second, executor.pending.get(secondId).project());
        assertEquals("  原始内容  ", executor.pending.get(firstId).content());
        assertNull(executor.pending.get(firstId).memory());
        assertEquals("先前记忆", executor.pending.get(secondId).memory());
        var confirm = new AgentExecResult();
        confirm.setConfirmationRequired(true);
        confirm.setConfirmationMessage("请确认范围");
        executor.complete(secondId, confirm, "第二次执行诊断");
        var success = new AgentExecResult();
        success.setSuccess(true);
        executor.complete(firstId, success, "第一次执行诊断");
        assertEquals(List.of(secondId, firstId), results.stream().map(AgentExecResult::getTaskId).toList());
        assertEquals("第二次执行诊断", executor.getStderr(secondId));
        assertEquals("第一次执行诊断", executor.getStderr(firstId));
        assertFalse(confirm.isSuccess());
        assertTrue(executor.pending.isEmpty());
    }

    @Test
    void rejectedSubmissionHasNoCompletionAndAcceptedFailureUsesAResult() {
        var executor = new ManualExecutor(new AgentBean());
        var results = new ArrayList<AgentExecResult>();
        executor.reject = true;
        assertThrows(RejectedExecutionException.class,
                () -> executor.exec(project("p"), "内容", "", results::add));
        assertTrue(results.isEmpty());
        assertTrue(executor.pending.isEmpty());
        executor.reject = false;
        String id = executor.exec(project("p"), "内容", "", results::add);
        var failure = new AgentExecResult();
        failure.setErrorMessage("测试失败");
        executor.complete(id, failure, "");
        assertEquals(1, results.size());
        assertEquals(id, results.getFirst().getTaskId());
        assertFalse(results.getFirst().isSuccess());
        assertFalse(results.getFirst().isConfirmationRequired());
    }

    @Test
    void projectPortCarriesProjectAndSelectedRequirements() {
        var port = new ProjectUserif() {
            @Override public Project newProject(String content) { return project(content); }
            @Override public List<Requirement> createRequirements(Project project, String content) {
                var requirement = new Requirement();
                requirement.setUserContent(content);
                project.getRequirements().add(requirement);
                return project.getRequirements();
            }
            @Override public List<Requirement> execTask(Project project, List<Requirement> requirements) {
                assertTrue(project.getRequirements().containsAll(requirements));
                return requirements;
            }
        };
        assertInstanceOf(UserInterface.class, port);
        Project project = port.newProject("项目");
        var requirements = port.createRequirements(project, "需求");
        assertSame(project.getRequirements(), requirements);
        assertSame(requirements, port.execTask(project, requirements));
    }

    private static Project project(String id) {
        var project = new Project();
        project.setProjectId(id);
        project.setWorkingDirectory(Path.of(id));
        return project;
    }

    // 只用于表达调用契约：测试手动触发完成，不注册组件，不证明生产异步执行或任务调度。
    private static final class ManualExecutor extends AgentExecutor {
        private final Map<String, Submission> pending = new HashMap<>();
        private final Map<String, String> diagnostics = new HashMap<>();
        private int sequence;
        private boolean reject;

        private ManualExecutor(AgentBean agent) { super(agent); }

        @Override
        public String exec(Project project, String content, String memory, AgentExecCallback callback) {
            if (reject) throw new RejectedExecutionException("测试拒绝提交");
            String id = "execution-" + ++sequence;
            pending.put(id, new Submission(project, content, memory, callback));
            return id;
        }

        @Override public String getStderr(String taskId) { return diagnostics.get(taskId); }

        private void complete(String id, AgentExecResult result, String diagnostic) {
            Submission submission = pending.remove(id);
            assertNotNull(submission);
            result.setTaskId(id);
            diagnostics.put(id, diagnostic);
            submission.callback().onCompleted(result);
        }
    }

    private record Submission(Project project, String content, String memory, AgentExecCallback callback) { }
}
