/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证只读规划的完整解析、任务标识及失败路径。
 */
package com.kk24426.zbagentwf.agent.codex;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.project.bean.TaskStatus;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexRequirementPlannerTest {
    @TempDir Path temp;

    @Test
    void createsMultipleRequirementsAndTasksPreservingOriginalInput() {
        var planner = new CodexRequirementPlanner(CodexFixtureSupport.client("success"));
        String content = "  用户原始输入\n中文  ";
        var requirements = planner.plan(temp, content);
        assertEquals(2, requirements.size());
        var ids = new HashSet<String>();
        for (var requirement : requirements) {
            assertEquals(content, requirement.getUserContent());
            assertEquals(2, requirement.getTasks().size());
            for (var task : requirement.getTasks()) {
                assertTrue(ids.add(task.getId()));
                assertEquals(TaskStatus.PENDING, task.getStatus());
                assertNull(task.getResult());
            }
        }
    }

    @Test
    void rejectsPartialOrInvalidPlanning() {
        var planner = new CodexRequirementPlanner(CodexFixtureSupport.client("bad-plan"));
        assertThrows(IllegalStateException.class, () -> planner.plan(temp, "原始需求"));
    }

    @Test
    void failedPlanningRetainsBoundedRedactedDiagnosticsWithoutChangingPublicMessage() {
        var planner = new CodexRequirementPlanner(CodexFixtureSupport.client("auth-failure"));
        var failure = assertThrows(IllegalStateException.class, () -> planner.plan(temp, "原始需求"));
        assertEquals("需求规划失败，原需求列表未修改。", failure.getMessage());
        assertTrue(failure.getCause().getMessage().contains("退出码=7"));
        assertEquals(1, failure.getCause().getSuppressed().length);
        String diagnostic = failure.getCause().getSuppressed()[0].getMessage();
        assertTrue(diagnostic.contains("fixture authentication failed"));
        assertFalse(diagnostic.contains("fixture-secret"));
    }
}
