/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证项目响应是独立且一致的不可变数据快照。
 */
package com.kk24426.zbagentwf.common.project.bean;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class ProjectHttpTest {
    @Test void concurrentReadersCannotMixStatusAndResultAndSnapshotsStayDetached() throws Exception {
        var project = new Project(); project.setProjectId("id");
        var requirement = new Requirement(); var task = new RequirementTask(); task.setId("task");
        requirement.getTasks().add(task); project.getRequirements().add(requirement);
        var before = ProjectHttp.view(project);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var writer = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 1000; i++) synchronized (project) {
                    if (i % 2 == 0) { var result = new AgentExecResult(); result.setSuccess(true);
                        task.setResult(result); task.setStatus(TaskStatus.SUCCEEDED);
                    } else { task.setResult(null); task.setStatus(TaskStatus.PENDING); }
                }
                return null;
            });
            var reader = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 1000; i++) {
                    var snapshot = ProjectHttp.view(project).requirements().getFirst().tasks().getFirst();
                    assertEquals(snapshot.status() == TaskStatus.SUCCEEDED, snapshot.result() != null);
                }
                return null;
            });
            start.countDown(); writer.get(5, TimeUnit.SECONDS); reader.get(5, TimeUnit.SECONDS);
        }
        project.getRequirements().clear();
        assertEquals(1, before.requirements().size());
        assertEquals(TaskStatus.PENDING, before.requirements().getFirst().tasks().getFirst().status());
        assertThrows(UnsupportedOperationException.class, () -> before.requirements().clear());
    }
}
