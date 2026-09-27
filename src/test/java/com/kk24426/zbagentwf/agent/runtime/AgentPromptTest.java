/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证提示词显式初始化、输入快照和缺规则时不启动进程。
 */
package com.kk24426.zbagentwf.agent.runtime;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.codex.*;
import com.kk24426.zbagentwf.agent.prompt.PromptCatalog;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.user.agent.api.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentPromptTest {
    @TempDir Path temp;

    @Test void abstractHooksAreNotCalledDuringConstruction() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var executor = new AgentExecutor(new AgentBean()) {
            protected Prompt getDefaultPrompt() { calls.incrementAndGet(); return null; }
            protected Prompt getSecurityPrompt() { calls.incrementAndGet(); return null; }
            public String exec(Project p, String c, String m, AgentExecutionCallback callback) { throw new AssertionError(); }
            public String getStderr(String id) { return null; }
        };
        assertNotNull(executor); assertEquals(0, calls.get());
    }

    @Test void explicitInitializationCopiesNestedInputsAndLatestProjectRulesAreUsed() {
        var bean = new AgentBean(); bean.setRolePrompt(AbstractAgentExecutor.prompt("原角色"));
        var user = AbstractAgentExecutor.prompt("用户规则");
        var executor = new Inspectable(bean, user, AbstractAgentExecutor.prompt("构造项目规则"));
        var project = new Project();
        assertThrows(AgentConfigurationUnavailableException.class, () -> executor.instructionsFor(project));
        assertThrows(AgentConfigurationUnavailableException.class,
                () -> executor.initializePrompts(AbstractAgentExecutor.prompt(" "), AbstractAgentExecutor.prompt("安全")));
        var defaults = AbstractAgentExecutor.prompt("通用"); var security = AbstractAgentExecutor.prompt("安全");
        executor.initializePrompts(defaults, security);
        defaults.setPrompt("外部变更"); security.setPrompt("外部变更");
        user.setPrompt("外部变更"); bean.getRolePrompt().setPrompt("外部变更");
        String initial = executor.instructionsFor(project);
        assertTrue(initial.contains("原角色")); assertTrue(initial.contains("用户规则"));
        assertTrue(initial.contains("构造项目规则")); assertFalse(initial.contains("外部变更"));
        assertTrue(initial.indexOf("安全规则") < initial.indexOf("通用规则"));
        project.setProjectPrompt(AbstractAgentExecutor.prompt("新项目规则"));
        String next = executor.instructionsFor(project);
        assertTrue(next.contains("新项目规则")); assertFalse(next.contains("构造项目规则"));
        assertThrows(IllegalStateException.class, () -> executor.initializePrompts(defaults, security));
    }

    @Test void absentBlankPlaceholderAndInvalidUtf8RulesCannotSilentlyExecute() throws Exception {
        var absent = PromptCatalog.load(temp.resolve("absent"));
        assertThrows(AgentConfigurationUnavailableException.class, () -> absent.require("default"));
        Files.writeString(temp.resolve("default.txt"), " ");
        Files.writeString(temp.resolve("security.txt"), "YOUR_SECURITY_RULES_REPLACE_BEFORE_USE");
        var empty = PromptCatalog.load(temp);
        assertThrows(AgentConfigurationUnavailableException.class, () -> empty.require("default"));
        assertThrows(AgentConfigurationUnavailableException.class, () -> empty.require("security"));
        Files.write(temp.resolve("default.txt"), new byte[]{(byte) 0xc3, 0x28});
        assertThrows(java.io.IOException.class, () -> PromptCatalog.load(temp));
        var project = new Project(); project.setWorkingDirectory(temp); project.setProjectId("fixture");
        try (var executor = new CodexAgentExecutor(new AgentBean(), CodexFixtureSupport.client("success"))) {
            assertThrows(RejectedExecutionException.class, () -> executor.exec(project, "内容", "", ignored -> fail()));
            assertFalse(Files.exists(temp.resolve("started")));
        }
        var planner = new CodexRequirementPlanner(CodexFixtureSupport.client("success"));
        assertThrows(AgentConfigurationUnavailableException.class, () -> planner.plan(temp, "内容"));
        assertFalse(Files.exists(temp.resolve("started")));
    }

    @Test void acceptedExecutionKeepsRulesSnapshotEvenIfProjectChanges() throws Exception {
        var bean = new AgentBean(); bean.setRolePrompt(AbstractAgentExecutor.prompt("开发角色"));
        var project = new Project(); project.setProjectId("fixture"); project.setWorkingDirectory(temp);
        project.setProjectPrompt(AbstractAgentExecutor.prompt("受理时项目规则"));
        try (var executor = CodexFixtureSupport.ready(new CodexAgentExecutor(bean, CodexFixtureSupport.client("success")))) {
            var done = new CompletableFuture<AgentExecutionResult>();
            executor.exec(project, "内容", "记忆", done::complete);
            project.setProjectPrompt(AbstractAgentExecutor.prompt("之后的项目规则"));
            var result = done.get(10, TimeUnit.SECONDS);
            assertTrue(result.isSuccess());
            assertTrue(result.getSummary().contains("开发角色"));
            assertTrue(result.getSummary().contains("受理时项目规则"));
            assertFalse(result.getSummary().contains("之后的项目规则"));
        }
    }

    private static class Inspectable extends AbstractAgentExecutor {
        Inspectable(AgentBean agent, Prompt user, Prompt project) { super(agent, user, project); }
        public String exec(Project p, String c, String m, AgentExecutionCallback callback) { throw new AssertionError(); }
        public String getStderr(String id) { return null; }
    }
}
