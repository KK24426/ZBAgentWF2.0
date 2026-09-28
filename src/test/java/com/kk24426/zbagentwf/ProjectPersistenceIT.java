/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：在专用MySQL中使用生产四表和Mapper验证项目聚合事务。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.persistence.ProjectDao;
import com.kk24426.zbagentwf.agent.persistence.mapper.ProjectMapper;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.logging.SecretRedactor;
import com.zaxxer.hikari.HikariDataSource;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.support.JdbcTransactionManager;

/** 仅显式mysql-it执行；只创建随机前缀四表，清理范围限于本测试成功创建的表。 */
@EnabledIfSystemProperty(named="mysql.it.enabled", matches="true")
class ProjectPersistenceIT {
    /** 真实驱动、生产DDL/SQL、短事务和重载共同验收，不使用测试内存Store。 */
    @Test void productionGraphRoundTripAtomicityAndOwnership() throws Exception {
        String url=required("ZB_TEST_DB_URL"), username=required("ZB_TEST_DB_USERNAME"), password=required("ZB_TEST_DB_PASSWORD");
        assertTrue(url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost)(?::[0-9]{1,5})?/zbagentwf_test"));
        SecretRedactor.register(username); SecretRedactor.register(password);
        String prefix="zb_it_"+UUID.randomUUID().toString().replace("-","");
        var created=new ArrayList<String>();
        try (var source=new HikariDataSource()) {
            source.setJdbcUrl(url); source.setUsername(username); source.setPassword(password);
            source.setMaximumPoolSize(3); source.setMinimumIdle(0); source.setConnectionTimeout(5000);
            source.addDataSourceProperty("connectTimeout","5000"); source.addDataSourceProperty("socketTimeout","30000");
            try (Connection connection=source.getConnection()) {
                assertEquals("zbagentwf_test",connection.getCatalog());
                try {
                    String ddl=renamed(resource("/db/project-schema.sql"),prefix).replaceAll("(?m)^--.*$","");
                    for (String sql:ddl.split(";")) if (!sql.isBlank()) {
                        String table=sql.strip().split("\\s+")[2];
                        assertTrue(table.matches(prefix+"_(agent|project|requirement|task)"));
                        try (var statement=connection.createStatement()) { statement.executeUpdate(sql); }
                        created.add(table);
                    }
                    var configuration=new Configuration(new Environment("project-test",new SpringManagedTransactionFactory(),source));
                    configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
                    String xml=renamed(resource("/mapper/ProjectMapper.xml"),prefix);
                    new XMLMapperBuilder(new StringReader(xml),configuration,"project-test-mapper",configuration.getSqlFragments()).parse();
                    var sessions=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration));
                    var mapper=sessions.getMapper(ProjectMapper.class);
                    var transactions=new JdbcTransactionManager(source);
                    var dao=new ProjectDao(mapper,transactions);
                    verify(dao,new ProjectDao(mapper,transactions));
                    verifyDeletedSlots(dao); verifySharedGraphs(dao); verifyProbeFailure(dao,mapper,transactions);
                } finally {
                    Collections.reverse(created);
                    for (String table:created) try (var statement=connection.createStatement()) { statement.executeUpdate("DROP TABLE "+table); }
                }
            }
        } catch (Exception failure) {
            var text=new java.io.StringWriter(); failure.printStackTrace(new java.io.PrintWriter(text));
            fail("项目四表验收失败："+SecretRedactor.redact(text.toString()));
        }
    }

    /** 第二个DAO模拟服务重建；验证提交后发布、冲突隔离、父子归属以及多项目整体回滚。 */
    private static void verify(ProjectDao dao,ProjectDao restarted) {
        Project project=project("持久化中文🙂");
        assertEquals(1,dao.insert(project)); assertNotNull(project.getId());
        var task=project.getRequirements().getFirst().getTasks().getFirst();
        assertNotNull(task.getId()); assertNotEquals(project.getPlanningAgent().getId(),project.getDevelopmentAgent().getId());
        Project loaded=restarted.findByProjectId(project.getProjectId());
        assertEquals("持久化中文🙂",loaded.getProjectName()); assertEquals("high",loaded.getPlanningAgent().getThink());
        assertEquals("项目规则",loaded.getProjectPrompt().getPrompt());
        assertEquals("第一项",loaded.getRequirements().getFirst().getTasks().getFirst().getContent());
        assertEquals("第二项",loaded.getRequirements().getFirst().getTasks().get(1).getContent());
        assertNotNull(loaded.getCreationData()); assertEquals(project.getId(),loaded.getId());
        Project stale=dao.selectById(project.getId());
        var result=new AgentExecutionResult(); result.setTaskId(UUID.randomUUID().toString()); result.setSuccess(true); result.setSummary("已完成🙂"); result.setTokenCount(42L);
        task.setStatus(TaskStatus.SUCCEEDED); task.setResult(result); project.setProjectName("更新名称");
        dao.update(project);
        loaded=restarted.selectById(project.getId());
        assertEquals(1,loaded.getVersion()); assertEquals(TaskStatus.SUCCEEDED,loaded.getRequirements().getFirst().getTasks().getFirst().getStatus());
        assertEquals(result.getTaskId(),loaded.getRequirements().getFirst().getTasks().getFirst().getResult().getTaskId());
        assertEquals(42L,loaded.getRequirements().getFirst().getTasks().getFirst().getResult().getTokenCount());
        assertThrows(IllegalStateException.class,()->dao.update(stale));
        assertEquals(0,stale.getVersion()); assertThrows(IllegalStateException.class,()->dao.requireReliable(stale));
        dao.refresh(stale,dao.selectById(stale.getId())); dao.requireReliable(stale);
        assertEquals("更新名称",stale.getProjectName());

        Project foreign=project("另一个项目"); dao.insert(foreign);
        Long foreignId=foreign.getRequirements().getFirst().getTasks().getFirst().getId();
        var injected=task("错误归属"); injected.setId(foreignId);
        stale.getRequirements().getFirst().getTasks().add(injected);
        int version=stale.getVersion();
        assertThrows(IllegalStateException.class,()->dao.update(stale));
        assertEquals(version,stale.getVersion()); assertEquals(version,dao.selectById(stale.getId()).getVersion());
        assertEquals(2,dao.selectById(stale.getId()).getRequirements().getFirst().getTasks().size());

        Project first=project("批量第一项"), duplicate=project("重复UUID"); duplicate.setProjectId(first.getProjectId());
        assertThrows(IllegalStateException.class,()->dao.insertAll(List.of(first,duplicate)));
        assertNull(first.getId()); assertNull(first.getPlanningAgent().getId()); assertNull(first.getRequirements().getFirst().getTasks().getFirst().getId());
        assertNull(dao.findByProjectId(first.getProjectId())); assertEquals(2,dao.selectAll().size());
        Project deleted=dao.selectById(foreign.getId()); deleted.setDelFlg(true); dao.update(deleted);
        assertNull(restarted.findByProjectId(foreign.getProjectId())); assertEquals(1,restarted.selectAll().size());
    }

    /** 删除行仍占唯一排序槽；同对象再次保存、重载后追加及执行状态更新都应继续成功。 */
    private static void verifyDeletedSlots(ProjectDao dao) {
        Project p=project("删除槽回归");var second=new Requirement();second.setUserContent("后续需求");second.getTasks().add(task("后续任务"));p.getRequirements().add(second);dao.insert(p);
        p.getRequirements().getFirst().getTasks().getFirst().setDelFlg(true);dao.update(p);dao.update(p);
        p=dao.selectById(p.getId());assertEquals(1,p.getRequirements().getFirst().getTasks().size());
        p.getRequirements().getFirst().getTasks().add(task("追加任务"));dao.update(p);
        p=dao.selectById(p.getId());assertEquals("第二项",p.getRequirements().getFirst().getTasks().getFirst().getContent());
        assertEquals("追加任务",p.getRequirements().getFirst().getTasks().getLast().getContent());
        p.getRequirements().getFirst().setDelFlg(true);dao.update(p);dao.update(p);
        p=dao.selectById(p.getId());assertEquals(1,p.getRequirements().size());assertEquals("后续需求",p.getRequirements().getFirst().getUserContent());
        var added=new Requirement();added.setUserContent("追加需求");added.getTasks().add(task("新增任务"));p.getRequirements().add(added);dao.update(p);
        p.getRequirements().getFirst().getTasks().getFirst().setStatus(TaskStatus.RUNNING);dao.update(p);
        assertEquals(2,dao.selectById(p.getId()).getRequirements().size());
        Project active=p;active.getPlanningAgent().setDelFlg(true);
        assertThrows(IllegalArgumentException.class,()->dao.update(active));
        assertNotNull(dao.selectById(active.getId()).getPlanningAgent());
    }

    /** 批量共享尚无主键的任一级实体都必须整体拒绝，数据库行数和原对象ID均不改变。 */
    private static void verifySharedGraphs(ProjectDao dao) {
        int count=dao.selectAll().size();
        for (String level:List.of("agent","requirement","task")) {
            Project first=project("共享第一项"), second=project("共享第二项");
            switch(level) {
                case "agent" -> second.setPlanningAgent(first.getPlanningAgent());
                case "requirement" -> second.setRequirements(first.getRequirements());
                default -> second.getRequirements().getFirst().setTasks(first.getRequirements().getFirst().getTasks());
            }
            assertThrows(IllegalArgumentException.class,()->dao.insertAll(List.of(first,second)),level);
            assertNull(first.getId());assertNull(second.getId());assertNull(first.getPlanningAgent().getId());
            assertEquals(count,dao.selectAll().size());
        }
    }

    /** 正式DAO已提交RUNNING，终态保存前checkSchema失败必须隔离；数据库重载保留RUNNING。 */
    private static void verifyProbeFailure(ProjectDao dao,ProjectMapper mapper,JdbcTransactionManager transactions) {
        var failProbe=new java.util.concurrent.atomic.AtomicBoolean();
        ProjectMapper wrapped=(ProjectMapper)java.lang.reflect.Proxy.newProxyInstance(ProjectMapper.class.getClassLoader(),new Class<?>[]{ProjectMapper.class},
                (proxy,method,args)->{
                    if (method.getName().equals("checkSchema") && failProbe.get()) throw new IllegalStateException("probe unavailable fixture");
                    try { return method.invoke(mapper,args); }
                    catch(java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                });
        var tested=new ProjectDao(wrapped,transactions);Project p=project("终态探测故障");tested.insert(p);
        var t=p.getRequirements().getFirst().getTasks().getFirst();t.setStatus(TaskStatus.RUNNING);tested.update(p);
        t.setStatus(TaskStatus.SUCCEEDED);failProbe.set(true);assertThrows(IllegalStateException.class,()->tested.update(p));
        assertThrows(IllegalStateException.class,()->tested.requireReliable(p));
        var stored=dao.findByProjectId(p.getProjectId());assertEquals(TaskStatus.RUNNING,stored.getRequirements().getFirst().getTasks().getFirst().getStatus());
        tested.refresh(p,stored);tested.requireReliable(p);
    }

    /** 构造独立三角色和有序需求任务，只提供业务数据，主键完全由真实数据库生成。 */
    private static Project project(String name) {
        var project=new Project(name,new ArrayList<>(),agent("规划"),agent("开发"),agent("审核"));
        project.setProjectId(UUID.randomUUID().toString()); project.setProjectPrompt(new Prompt("项目规则"));
        var requirement=new Requirement(); requirement.setUserContent("用户需求"); requirement.setAgentUnderstanding("完整理解"); requirement.setAcceptanceCriteria("验收");
        requirement.getTasks().add(task("第一项")); requirement.getTasks().add(task("第二项")); project.getRequirements().add(requirement); return project;
    }
    /** 每个角色均为独立快照，覆盖think、角色提示词及skill序列化。 */
    private static AgentBean agent(String role) {
        var bean=new AgentBean(); bean.setBrand("Codex"); bean.setName("test"); bean.setVer("1"); bean.setThink("high"); bean.setRolePrompt(new Prompt(role));
        var skill=new Skill(); skill.setSkillName("测试技能"); bean.setSkill(skill); return bean;
    }
    /** 新任务不预设数据库主键。 */
    private static RequirementTask task(String text) { var task=new RequirementTask(); task.setContent(text); task.setAcceptanceCriteria("可验证"); return task; }
    /** 必需环境值仅在内存读取，缺少配置时失败，不能将显式验收标记为跳过。 */
    private static String required(String name) { String value=System.getenv(name); assertNotNull(value,"缺少环境变量 "+name); assertFalse(value.isBlank()); return value; }
    /** 读取正式资源，不在测试复制另一套schema或SQL。 */
    private static String resource(String path) throws Exception {
        try (var stream=Objects.requireNonNull(ProjectPersistenceIT.class.getResourceAsStream(path))) { return new String(stream.readAllBytes(),StandardCharsets.UTF_8); }
    }
    /** 只替换固定表名的完整单词；表名来自本测试随机常量，不提供生产动态SQL入口。 */
    private static String renamed(String text,String prefix) {
        for (String suffix:List.of("agent","project","requirement","task")) text=text.replaceAll("\\bzb_"+suffix+"\\b",prefix+"_"+suffix);
        return text;
    }
}
