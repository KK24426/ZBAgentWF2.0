/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证项目、需求、Task 的包含关系、默认值及普通属性存取。
 */
package com.kk24426.zbagentwf.common.project.bean;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ProjectModelTest {
    @Test
    void projectsOwnMultipleRequirementsAndEachRequirementOwnsMultipleTasks() {
        var first = new Project();
        var second = new Project();
        var requirement = new Requirement();
        var another = new Requirement();
        first.getRequirements().add(requirement);
        first.getRequirements().add(another);
        requirement.getTasks().add(new RequirementTask());
        requirement.getTasks().add(new RequirementTask());
        another.getTasks().add(new RequirementTask());
        another.getTasks().add(new RequirementTask());

        assertEquals(2, first.getRequirements().size());
        assertEquals(2, first.getRequirements().getFirst().getTasks().size());
        assertEquals(2, first.getRequirements().getLast().getTasks().size());
        assertTrue(second.getRequirements().isEmpty());
        assertNotSame(first.getRequirements(), second.getRequirements());
        assertNotSame(requirement.getTasks(), another.getTasks());
        requirement.getTasks().clear();
        assertEquals(2, another.getTasks().size());
    }

    @Test
    void planningTaskAndExecutionResultKeepDistinctIdentifiers() {
        var project = new Project();
        project.setProjectId("项目一");
        project.setWorkingDirectory(Path.of("工作目录"));
        var requirements = new ArrayList<Requirement>();
        project.setRequirements(requirements);
        var requirement = new Requirement();
        requirement.setUserContent("  原始需求\n第二行");
        requirement.setAgentUnderstanding("需求理解");
        requirement.setAcceptanceCriteria("需求验收");
        requirement.setUserConfirmMsg("待确认范围");
        var tasks = new ArrayList<RequirementTask>();
        requirement.setTasks(tasks);
        requirements.add(requirement);
        var task = new RequirementTask();
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNull(task.getResult());
        task.setId("planning-1");
        task.setContent("执行内容");
        task.setAcceptanceCriteria("任务验收");
        var result = new AgentExecResult();
        assertNull(result.getTokenCount());
        result.setTaskId("execution-2");
        result.setConfirmationRequired(true);
        result.setConfirmationMessage("请确认工作范围");
        result.setSummary("等待确认，本次执行结束");
        task.setStatus(TaskStatus.NEEDS_CONFIRMATION);
        task.setResult(result);
        tasks.add(task);

        assertEquals("项目一", project.getProjectId());
        assertEquals(Path.of("工作目录"), project.getWorkingDirectory());
        assertSame(requirements, project.getRequirements());
        assertEquals("  原始需求\n第二行", requirement.getUserContent());
        assertEquals("需求理解", requirement.getAgentUnderstanding());
        assertEquals("需求验收", requirement.getAcceptanceCriteria());
        assertEquals("待确认范围", requirement.getUserConfirmMsg());
        assertSame(tasks, requirement.getTasks());
        assertEquals("planning-1", task.getId());
        assertEquals("执行内容", task.getContent());
        assertEquals("任务验收", task.getAcceptanceCriteria());
        assertEquals(TaskStatus.NEEDS_CONFIRMATION, task.getStatus());
        assertSame(result, task.getResult());
        assertEquals("execution-2", task.getResult().getTaskId());
        assertFalse(result.isSuccess());
        assertTrue(result.isConfirmationRequired());
        assertEquals("请确认工作范围", result.getConfirmationMessage());
        assertEquals("等待确认，本次执行结束", result.getSummary());
    }

    @Test
    void resultPropertiesPreserveValuesWithoutInferringBusinessState() {
        var result = new AgentExecResult();
        result.setSuccess(true);
        result.setTokenCount(0L);
        result.setSummary("完成");
        assertTrue(result.isSuccess());
        assertFalse(result.isConfirmationRequired());
        assertEquals(0L, result.getTokenCount());
        assertEquals("完成", result.getSummary());
        result.setSuccess(false);
        result.setErrorMessage("工具执行失败");
        result.setTokenCount(null);
        assertFalse(result.isSuccess());
        assertEquals("工具执行失败", result.getErrorMessage());
        assertNull(result.getTokenCount());
        var requirement = new Requirement();
        requirement.setUserContent("");
        requirement.setAgentUnderstanding(null);
        assertEquals("", requirement.getUserContent());
        assertNull(requirement.getAgentUnderstanding());
    }
}
