/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证真实ProjectDao在SQL前失败时的隔离与聚合共享拒绝。
 */
package com.kk24426.zbagentwf.agent.persistence;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.kk24426.zbagentwf.agent.persistence.mapper.ProjectMapper;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.project.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

/** 直接调用正式DAO；Mapper只模拟边界故障，真实SQL另由ProjectPersistenceIT验收。 */
class ProjectDaoTest {
    /** 终态候选已经改在内存，但保存前数据库探测失败，仍必须隔离并阻止假成功。 */
    @Test void schemaCheckFailureQuarantinesUnsavedTerminalCandidate() {
        var mapper=mock(ProjectMapper.class);var transactions=mock(PlatformTransactionManager.class);
        var dao=new ProjectDao(mapper,transactions);var project=project();project.setId(1L);
        project.getRequirements().getFirst().getTasks().getFirst().setStatus(TaskStatus.SUCCEEDED);
        when(mapper.checkSchema()).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class,()->dao.update(project));
        assertThrows(IllegalStateException.class,()->dao.requireReliable(project));
        verifyNoInteractions(transactions);verify(mapper).checkSchema();
    }
    /** 即使实体还没有ID，两个项目也不能共享角色、需求或任务，否则提交后回填会错位。 */
    @Test void sharedBatchEntitiesAreRejectedBeforeAnySql() {
        for (String level:List.of("role","requirement","task")) {
            var mapper=mock(ProjectMapper.class);var transactions=mock(PlatformTransactionManager.class);
            var dao=new ProjectDao(mapper,transactions);var a=project();var b=project();
            switch(level) {
                case "role" -> b.setPlanningAgent(a.getPlanningAgent());
                case "requirement" -> b.setRequirements(a.getRequirements());
                default -> b.getRequirements().getFirst().setTasks(a.getRequirements().getFirst().getTasks());
            }
            assertThrows(IllegalArgumentException.class,()->dao.insertAll(List.of(a,b)),level);
            verifyNoInteractions(mapper,transactions);assertNull(a.getId());assertNull(b.getId());
        }
    }
    /** 构造无数据库身份的独立可变聚合，不附带目录或进程。 */
    private static Project project() {
        var project=new Project("test",new ArrayList<>(),new AgentBean(),new AgentBean(),new AgentBean());
        project.setProjectId(UUID.randomUUID().toString());var requirement=new Requirement();
        var task=new RequirementTask();task.setContent("任务");requirement.getTasks().add(task);project.getRequirements().add(requirement);return project;
    }
}
