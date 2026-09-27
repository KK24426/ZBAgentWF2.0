/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证项目登记、角色选择、提示词追加及失败不发布。
 */
package com.kk24426.zbagentwf.agent.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.kk24426.zbagentwf.agent.codex.CodexFixtureSupport;
import com.kk24426.zbagentwf.agent.registry.*;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.memory.MemoryStore;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.project.config.ProjectSettings;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.user.agent.api.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectMemoryTest {
    @TempDir Path temp;

    @Test void createsDistinctBindingsRegistersOriginalAndAppendsProjectPrompt() {
        var memory = new MemoryStore();
        var received = new ArrayList<AgentBean>();
        AgentExecutorFactory factory = bean -> { received.add(bean); return mock(ProjectAgentExecutor.class); };
        var planner = mock(AgentRequirementPlanner.class);
        when(planner.plan(any(), anyString())).thenReturn(List.of(new Requirement()));
        var selection = new RoleAgentResolver.Selection("provider", "model", "default");
        var roles = new RoleAgentResolver(Map.of("planning", selection, "development", selection, "review", selection),
                CodexFixtureSupport.prompts(temp));
        var service = new ProjectDomainImpl(new ProjectSettings(temp.resolve("projects")), factory, planner, memory, roles);
        var first = service.newProject("第一项目");
        assertSame(first, service.getProject(first.getProjectId()));
        assertEquals(List.of("default", "default", "default"), received.stream().map(AgentBean::getVer).toList());
        assertEquals(List.of("fixture-planning-规则", "fixture-development-规则", "fixture-review-规则"),
                received.stream().map(a -> a.getRolePrompt().getPrompt()).toList());
        assertNotSame(received.get(0), received.get(1));
        var selected = new AgentBean(); selected.setBrand("provider"); selected.setName("model"); selected.setVer("selected");
        var second = service.newProject("第二项目", selected, selected, selected);
        assertNotSame(first.getPlanningAgent(), second.getPlanningAgent());
        assertTrue(received.subList(3, 6).stream().allMatch(a -> a.getVer().equals("selected")));
        assertNull(selected.getRolePrompt(), "不得给调用者的模型 Bean 偷偷写角色规则");
        service.addProjectPrompt(first.getProjectId(), "规则一"); service.addProjectPrompt(first.getProjectId(), "规则二");
        assertEquals("规则一\n规则二", first.getProjectPrompt().getPrompt());
        assertNull(second.getProjectPrompt());
        assertThrows(IllegalArgumentException.class, () -> service.getProject("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.getProject(" "));
        assertThrows(IllegalArgumentException.class, () -> service.addProjectPrompt(first.getProjectId(), " "));
    }

    @Test void failedCreationNeverRegistersAndMissingDefaultsDoNotCreateDirectories() {
        var memory = new MemoryStore(); var planner = mock(AgentRequirementPlanner.class);
        var roles = new RoleAgentResolver(Map.of(), CodexFixtureSupport.prompts(temp));
        var service = new ProjectDomainImpl(new ProjectSettings(temp.resolve("projects")),
                ignored -> mock(ProjectAgentExecutor.class), planner, memory, roles);
        assertThrows(AgentConfigurationUnavailableException.class, () -> service.newProject("任务"));
        assertFalse(Files.exists(temp.resolve("projects")));
        var captured = new Project[1];
        when(planner.plan(any(), anyString())).thenAnswer(call -> {
            captured[0] = call.getArgument(0); throw new IllegalStateException("fixture failure");
        });
        var model = new AgentBean();
        assertThrows(IllegalStateException.class, () -> service.newProject("任务", model, model, model));
        assertNotNull(captured[0]);
        assertTrue(memory.get("project", captured[0].getProjectId(), Project.class).isEmpty());
        assertFalse(Files.exists(captured[0].getWorkingDirectory()));
    }

    @Test void roleConfigurationStillRejectsUppercaseMixedCaseAndUnknownKeys() {
        var selection = new RoleAgentResolver.Selection("provider", "model", "default");
        var prompts = CodexFixtureSupport.prompts(temp);
        for (String invalid : List.of("PLANNING", "DEVELOPMENT", "REVIEW", "Planning", " planning", "unknown")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new RoleAgentResolver(Map.of(invalid, selection), prompts), invalid);
        }
    }
    @Test void initialPlanningRejectionPreservesRetryableTypeAndDoesNotPublishProject() {
        var memory = new MemoryStore(); var planner = mock(AgentRequirementPlanner.class);
        var captured = new Project[1];
        when(planner.plan(any(), anyString())).thenAnswer(call -> {
            captured[0] = call.getArgument(0); throw new java.util.concurrent.RejectedExecutionException("fixture-full");
        });
        var service = new ProjectDomainImpl(new ProjectSettings(temp.resolve("projects")),
                ignored -> mock(ProjectAgentExecutor.class), planner, memory,
                new RoleAgentResolver(Map.of(), CodexFixtureSupport.prompts(temp)));
        var model = new AgentBean();
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> service.newProject("任务", model, model, model));
        assertFalse(Files.exists(captured[0].getWorkingDirectory()));
        assertTrue(memory.get("project", captured[0].getProjectId(), Project.class).isEmpty());
    }

}
