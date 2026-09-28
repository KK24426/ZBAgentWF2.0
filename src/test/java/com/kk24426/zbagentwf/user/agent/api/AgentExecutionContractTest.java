/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：验证Long任务选择只作用于本项目目标任务及回调执行身份。
 */
package com.kk24426.zbagentwf.user.agent.api;
import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.registry.ProjectFixtureSupport;
import java.nio.file.Path;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** 验证Long任务选择只作用于本项目目标任务及回调执行身份。 */
class AgentExecutionContractTest {
    @TempDir Path temp;
    /** 选择本项目某一Long任务只执行该项，拒绝跨项目、空ID及已成功任务。 */
    @Test void singleTaskSelectionCannotCrossProjectAndUsesIndependentExecutionIds() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("projects"),"success","success")) {
            var first=env.single();var second=env.single();var executor=env.factory.getExecutor(first);
            assertThrows(RejectedExecutionException.class,() -> executor.execTasks(second.getRequirements().getFirst().getTasks().getFirst().getId(),r -> fail()));
            assertThrows(RejectedExecutionException.class,() -> executor.execTasks(null,r -> fail()));
            var untouched=new com.kk24426.zbagentwf.common.project.model.RequirementTask(); untouched.setContent("其它待办");
            first.getRequirements().getFirst().getTasks().add(untouched);env.store.update(first);
            var done=new CompletableFuture<com.kk24426.zbagentwf.common.agent.model.AgentExecutionResult>();
            String id=executor.execTasks(first.getRequirements().getFirst().getTasks().getFirst().getId(),done::complete);
            assertEquals(id,done.get(20,TimeUnit.SECONDS).getTaskId());
            assertEquals(com.kk24426.zbagentwf.common.project.model.TaskStatus.PENDING,untouched.getStatus());
            assertNull(untouched.getResult());
            assertThrows(RejectedExecutionException.class,() -> executor.execTasks(first.getRequirements().getFirst().getTasks().getFirst().getId(),r -> fail()));
        }
    }
}
