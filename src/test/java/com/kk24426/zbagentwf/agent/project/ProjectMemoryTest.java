/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：验证数据库重载与canonical身份，防止把旧内存缓存当成事实来源。
 */
package com.kk24426.zbagentwf.agent.project;
import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.registry.ProjectFixtureSupport;
import com.kk24426.zbagentwf.agent.persistence.ProjectDao;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import java.nio.file.Path;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** 旧内存项目测试已迁移为持久化重载/canonical身份测试。 */
class ProjectMemoryTest {
    @TempDir Path temp;
    /** 数据库替身快照覆盖未保存修改，并发查询保持同一协调器身份。 */
    @Test void reloadUsesStoredSnapshotAndConcurrentLookupsShareIdentity() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("projects"),"success","success");var callers=Executors.newVirtualThreadPerTaskExecutor()) {
            var p=env.single();p.setProjectName("尚未保存");var canonical=env.domain.getProject(p.getProjectId());assertNotSame(p,canonical);assertEquals("fixture",canonical.getProjectName());
            var executor=env.factory.getExecutor(canonical);var work=new java.util.ArrayList<Future<?>>();
            for(int i=0;i<16;i++)work.add(callers.submit(() -> {var found=env.domain.getProject(p.getProjectId());assertSame(canonical,found);assertSame(executor,env.factory.getExecutor(found));}));
            for(var future:work)future.get(5,TimeUnit.SECONDS);
        }
    }
    /** 新领域实例首次并发查询同一已保存项目，只能发布一个运行时身份。 */
    @Test void concurrentFirstLoadsPublishOneCanonicalObject() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("projects"),"success","success");var callers=Executors.newVirtualThreadPerTaskExecutor()) {
            var p=env.single();var start=new CountDownLatch(1);
            var work=new java.util.ArrayList<Future<com.kk24426.zbagentwf.common.project.model.Project>>();
            for(int i=0;i<16;i++)work.add(callers.submit(()->{start.await();return env.domain.getProject(p.getProjectId());}));
            start.countDown();var first=work.getFirst().get(5,TimeUnit.SECONDS);
            for(var future:work)assertSame(first,future.get(5,TimeUnit.SECONDS));
        }
    }
    /** 无数据库配置时显式未就绪，不返回临时内存项目。 */
    @Test void absentMysqlNeverFallsBackToVolatileProjectStorage() {
        var dao=new ProjectDao(null,null);assertThrows(AgentConfigurationUnavailableException.class,dao::requireAvailable);
        assertThrows(AgentConfigurationUnavailableException.class,() -> dao.findByProjectId("anything"));
    }
}
