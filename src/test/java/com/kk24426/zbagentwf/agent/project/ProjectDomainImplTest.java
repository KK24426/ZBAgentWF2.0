/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：以深拷贝数据库替身验证项目规划、保存边界和失败停止，不代替真实SQL验收。
 */
package com.kk24426.zbagentwf.agent.project;
import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.registry.ProjectFixtureSupport;
import com.kk24426.zbagentwf.common.project.model.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** 以深拷贝数据库替身验证项目规划、保存边界和失败停止，不代替真实SQL验收。 */
class ProjectDomainImplTest {
    @TempDir Path temp;
    /** 创建和追加使用规划角色两阶段，开发按顺序执行且目录位于root/local/UUID。 */
    @Test void createPlanSplitSaveAppendAndExecuteUseCorrectRolesAndDirectory() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("projects"),"success","success")) {
            var p=env.create();assertEquals("测试项目",p.getProjectName());
            assertEquals("plan",p.getPlanningAgent().getVer());assertEquals("dev",p.getDevelopmentAgent().getVer());assertEquals("review",p.getReviewAgent().getVer());
            assertEquals(temp.resolve("projects/local").resolve(p.getProjectId()).toRealPath(),env.settings.directory(p.getProjectId()));
            assertEquals(2,p.getRequirements().size());assertEquals(2,p.getRequirements().getFirst().getTasks().size());
            assertNotNull(p.getRequirements().getFirst().getTasks().getFirst().getId());
            var added=env.domain.createRequiremens(p,"第二次需求");assertEquals(2,added.size());assertEquals(4,p.getRequirements().size());
            env.domain.addProjectPrompt(p.getProjectId(),"只操作项目目录");
            var done=CompletableFuture.supplyAsync(() -> env.domain.execTask(p));done.get(35,TimeUnit.SECONDS);
            assertTrue(p.getRequirements().stream().flatMap(r -> r.getTasks().stream()).allMatch(t -> t.getStatus()==TaskStatus.SUCCEEDED));
            assertSame(p,env.domain.getProject(p.getProjectId()));assertEquals(4,p.getRequirements().size());
        }
    }
    /** 首个失败停止后续待办，再次请求也不自动跨过失败或重复调用模型。 */
    @Test void batchStopsOnFirstFailureAndDoesNotRetryOnNextRequest() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("failure"),"success","failure")) {
            var p=env.create();var callback=new CompletableFuture<com.kk24426.zbagentwf.common.agent.model.AgentExecutionResult>();
            String batch=env.factory.getExecutor(p).execAllTasks(callback::complete);var result=callback.get(20,TimeUnit.SECONDS);
            var tasks=p.getRequirements().getFirst().getTasks();assertFalse(result.isSuccess());assertEquals(batch,result.getTaskId());
            assertNotEquals(batch,tasks.getFirst().getResult().getTaskId());assertEquals(TaskStatus.FAILED,tasks.getFirst().getStatus());
            assertEquals(TaskStatus.PENDING,tasks.getLast().getStatus());int calls=Files.readAllLines(env.settings.path(p.getProjectId()).resolve("business-count")).size();
            env.domain.execTask(p);assertEquals(calls,Files.readAllLines(env.settings.path(p.getProjectId()).resolve("business-count")).size());
        }
    }
    /** 模型完成但保存失败隔离对象，重载RUNNING后仍不自动重跑。 */
    @Test void saveFailureAfterExecutionRequiresReloadAndNeverReexecutesRunningTask() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("save"),"success","success")) {
            var p=env.create();env.store.failAt=env.store.saves+2;
            assertThrows(IllegalStateException.class,() -> env.domain.execTask(p));
            assertThrows(IllegalStateException.class,() -> env.store.requireReliable(p));
            var refreshed=env.domain.getProject(p.getProjectId());assertEquals(TaskStatus.RUNNING,refreshed.getRequirements().getFirst().getTasks().getFirst().getStatus());
            int calls=Files.readAllLines(env.settings.path(p.getProjectId()).resolve("business-count")).size();
            env.domain.execTask(refreshed);assertEquals(calls,Files.readAllLines(env.settings.path(p.getProjectId()).resolve("business-count")).size());
        }
    }
    /** RUNNING保存失败必须阻止实际模型调用。 */
    @Test void preExecutionPersistenceFailureDoesNotCallBusinessModel() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("before"),"success","success")) {
            var p=env.create();Path count=env.settings.path(p.getProjectId()).resolve("business-count");int calls=Files.readAllLines(count).size();
            env.store.failAt=env.store.saves+1;assertThrows(IllegalStateException.class,() -> env.domain.execTask(p));assertEquals(calls,Files.readAllLines(count).size());
        }
    }
    /** 第二阶段失败不保存局部需求；项目ID不能含路径穿越内容。 */
    @Test void secondPhaseFailureDoesNotSavePartialProjectAndUnsupportedPathsAreRejected() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("bad"),"bad-tasks","success")) {
            assertThrows(IllegalStateException.class,env::create);assertEquals(0,env.store.saves);
            assertThrows(IllegalArgumentException.class,() -> env.settings.path("../outside"));
        }
    }
    /** 即使模拟保存失败，进入持久化后目录也保留，避免误删可能已经提交的项目。 */
    @Test void creationKeepsDirectoryAfterPersistenceAttempt() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("projects"),"success","success")) {
            env.store.failAt=1;assertThrows(IllegalStateException.class,env::create);
            try(var entries=java.nio.file.Files.list(env.settings.getRootDirectory().resolve("local"))) {assertEquals(1,entries.count());}
        }
    }
}
