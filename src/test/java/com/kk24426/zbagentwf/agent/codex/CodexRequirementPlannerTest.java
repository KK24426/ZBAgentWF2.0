/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：验证只读需求/任务两阶段及审核失败不发布数据。
 */
package com.kk24426.zbagentwf.agent.codex;
import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** 验证只读需求/任务两阶段及审核失败不发布数据。 */
class CodexRequirementPlannerTest {
    @TempDir Path temp;
    /** 两阶段各自审核且各自只读，规划结果尚无数据库主键，已有任务不得覆盖。 */
    @Test void phasesUseIndependentReadOnlyCallsAndPublishNoIds() throws Exception {
        try(var resources=new ExecutionResources()) {
            var planner=new CodexRequirementPlanner(CodexFixtureSupport.client("success"),resources,new Object());
            String rules="只读规划；需求阶段仅requirements，任务阶段仅tasks；严格遵守本次schema。";
            var requirements=planner.plan(temp,"原始需求",rules);
            assertEquals(2,requirements.size());assertTrue(requirements.getFirst().getTasks().isEmpty());
            var tasks=planner.tasks(temp,requirements.getFirst(),rules);assertEquals(2,tasks.size());assertNull(tasks.getFirst().getId());
            assertEquals(2,Files.readAllLines(temp.resolve("audit-count")).size());
            assertEquals(2,Files.readAllLines(temp.resolve("business-count")).size());
            requirements.getFirst().setTasks(tasks);assertThrows(IllegalArgumentException.class,() -> planner.tasks(temp,requirements.getFirst(),rules));
        }
    }
    /** 需求格式错误或审核失败不返回部分规划，审核失败没有业务进程。 */
    @Test void malformedPhaseAndFailedSafetyReviewPublishNothing() throws Exception {
        for(String scenario:java.util.List.of("bad-plan","audit-deny","audit-bad"))try(var resources=new ExecutionResources()) {
            var dir=Files.createDirectory(temp.resolve(scenario));var planner=new CodexRequirementPlanner(CodexFixtureSupport.client(scenario),resources,new Object());
            assertThrows(IllegalStateException.class,() -> planner.plan(dir,"需求","rules"));
            if(scenario.startsWith("audit"))assertFalse(Files.exists(dir.resolve("business-count")));
        }
    }
}
