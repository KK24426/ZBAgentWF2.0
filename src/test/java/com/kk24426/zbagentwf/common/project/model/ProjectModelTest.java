/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证项目、需求、Task 的包含关系、默认值及普通属性存取。
 */
package com.kk24426.zbagentwf.common.project.model;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.agent.model.AgentExecutionResult;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** 验证用户数据结构和Long主键迁移，普通Bean本身不执行业务。 */
class ProjectModelTest {
    /** 验证项目与需求的集合各自独立，修改一条需求不会影响其它项目或需求。 */
    @Test
    void projectsOwnMultipleRequirementsAndEachRequirementOwnsMultipleTasks() {
        var first = new Project(null, new java.util.ArrayList<>(), null, null, null);
        var second = new Project(null, new java.util.ArrayList<>(), null, null, null);
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

    /** 验证任务Long主键与单次执行ID相互独立，Bean保留原始文本、引用及待确认结果而不推断业务状态。 */
    @Test
    void planningTaskAndExecutionResultKeepDistinctIdentifiers() {
        var project = new Project(null, new java.util.ArrayList<>(), null, null, null);
        project.setProjectId("项目一");
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
        task.setId(1L);
        task.setContent("执行内容");
        task.setAcceptanceCriteria("任务验收");
        var result = new AgentExecutionResult();
        assertNull(result.getTokenCount());
        result.setTaskId("execution-2");
        result.setConfirmationRequired(true);
        result.setConfirmationMessage("请确认工作范围");
        result.setSummary("等待确认，本次执行结束");
        task.setStatus(TaskStatus.NEEDS_CONFIRMATION);
        task.setResult(result);
        tasks.add(task);

        assertEquals("项目一", project.getProjectId());
        assertSame(requirements, project.getRequirements());
        assertEquals("  原始需求\n第二行", requirement.getUserContent());
        assertEquals("需求理解", requirement.getAgentUnderstanding());
        assertEquals("需求验收", requirement.getAcceptanceCriteria());
        assertEquals("待确认范围", requirement.getUserConfirmMsg());
        assertSame(tasks, requirement.getTasks());
        assertEquals(1L, task.getId());
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
        var result = new AgentExecutionResult();
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
