/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：为项目流程测试提供显式数据库替身与真实Java进程fixture。
 */
package com.kk24426.zbagentwf.agent.registry;
import com.kk24426.zbagentwf.agent.codex.*;
import com.kk24426.zbagentwf.agent.project.ProjectDomainImpl;
import com.kk24426.zbagentwf.agent.persistence.ProjectDao;
import com.kk24426.zbagentwf.agent.runtime.*;
import com.kk24426.zbagentwf.common.DataBean;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.project.config.ProjectSettings;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 测试替身不进入正式JAR，不代表真实MySQL验证。 */
public final class ProjectFixtureSupport implements AutoCloseable {
    public final Store store=new Store();
    public final ProjectSettings settings;
    public final AgentExecutorFactoryImpl factory;
    public final ProjectDomainImpl domain;
    /** planning/development分别选择fixture行为，审核行为由同一进程按schema区分。 */
    public ProjectFixtureSupport(Path root,String planning,String development) {
        settings=new ProjectSettings(root);
        var definitions=List.of(definition("plan"),definition("dev"),definition("review"));
        var catalog=new AgentCatalog(definitions);catalog.initialize();
        var prompts=CodexFixtureSupport.prompts(root.getParent());
        factory=new AgentExecutorFactoryImpl(catalog,prompts,settings,store,new ExecutionResources(),
            value -> CodexFixtureSupport.client(value.ver().equals("plan")?planning:development,Duration.ofSeconds(8),value.model()));
        var roles=new RoleAgentResolver(Map.of("planning",new RoleAgentResolver.Selection("provider","model","plan"),
            "development",new RoleAgentResolver.Selection("provider","model","dev"),"review",new RoleAgentResolver.Selection("provider","model","review")),prompts);
        domain=new ProjectDomainImpl(settings,factory,new AgentRequirementPlanner(factory),store,roles);
    }
    /** 创建已明确要求两阶段规划的项目。 */
    public Project create() { return domain.newProject("只实现指定功能，不增加业务规则","测试项目"); }
    /** 构造有一个已保存PENDING任务的项目，便于只验证执行协议。 */
    public Project single() throws Exception {
        Project p=new Project("fixture",new ArrayList<>(),definition("plan").bean(),definition("dev").bean(),definition("review").bean());
        for (AgentBean a:List.of(p.getPlanningAgent(),p.getDevelopmentAgent(),p.getReviewAgent())) a.setRolePrompt(new Prompt("fixture role"));
        p.setProjectId(UUID.randomUUID().toString()); settings.create(p.getProjectId());
        var r=new Requirement();r.setUserContent("原始内容");r.setAgentUnderstanding("理解");r.setAcceptanceCriteria("验收");
        var t=new RequirementTask();t.setContent("中文任务 ' ; $(literal)");t.setAcceptanceCriteria("验收");r.getTasks().add(t);p.getRequirements().add(r);store.insert(p);return p;
    }
    /** 仅使用现有Java可执行文件作为本地协议测试目标。 */
    public static AgentDefinition definition(String version) {
        String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        return new AgentDefinition("provider","model",version,"codex",java,"fixture-"+version,Duration.ofSeconds(8),true);
    }
    @Override public void close() { factory.close(); }

    /** 内存数据库替身显式深拷贝，模拟重载和指定保存失败；不伪装生产降级实现。 */
    public static final class Store extends ProjectDao {
        private final Map<String,Project> saved=new ConcurrentHashMap<>();
        private final Set<Project> uncertain=Collections.newSetFromMap(new IdentityHashMap<>());
        private long sequence;
        /** 已尝试保存次数，用于区分执行前与执行后失败。 */
        public int saves;
        /** 指定第几次保存失败；-1表示不注入故障。 */
        public int failAt=-1;
        /** 不连接真实数据库；仅由测试环境显式注入此替身。 */
        public Store() { super(null,null); }
        /** 测试内Store始终可用；正式ProjectDao不允许此降级。 */
        @Override public void requireAvailable() { }
        /** 模拟提交不确定后的隔离，成功重载之前禁止再次写入。 */
        @Override public synchronized void requireReliable(Project p) { if(uncertain.contains(p)) throw new IllegalStateException("fixture unpersisted"); }
        /** 与生产DAO一致，仅成功发布重载对象后解除隔离。 */
        @Override public synchronized void reloaded(Project p) { uncertain.remove(p); }
        @Override public synchronized int insert(Project p) { return save(p); }
        @Override public synchronized int update(Project p) { return save(p); }
        /** 可控制提交失败，在失败时不发布ID或保存快照。 */
        private int save(Project p) {
            requireReliable(p);
            if (++saves==failAt) { uncertain.add(p); throw new IllegalStateException("fixture persistence failed"); }
            meta(p);for(AgentBean a:List.of(p.getPlanningAgent(),p.getDevelopmentAgent(),p.getReviewAgent()))meta(a);
            for(Requirement r:p.getRequirements()) { meta(r);for(RequirementTask t:r.getTasks())meta(t); }
            saved.put(p.getProjectId(),copy(p));return 1;
        }
        /** 模拟数据库生成ID和版本，仅供流程断言，不声称具有SQL事务。 */
        private void meta(DataBean b) { if(b.getId()==null)b.setId(++sequence);else b.setVersion(b.getVersion()+1); }
        /** 每次返回独立图，避免同一引用掩盖读取与回滚错误。 */
        @Override public Project findByProjectId(String id) { Project p=saved.get(id);return p==null?null:copy(p); }
        /** 模拟跨连接读取，避免复用内存对象让重启/回滚测试假通过。 */
        public static Project copy(Project from) {
            var p=new Project(from.getProjectName(),new ArrayList<>(),agent(from.getPlanningAgent()),agent(from.getDevelopmentAgent()),agent(from.getReviewAgent()));
            copyMeta(from,p);p.setProjectId(from.getProjectId());p.setProjectPrompt(from.getProjectPrompt());
            for(Requirement old:from.getRequirements()) {
                var r=new Requirement();copyMeta(old,r);r.setUserContent(old.getUserContent());r.setAgentUnderstanding(old.getAgentUnderstanding());
                r.setAcceptanceCriteria(old.getAcceptanceCriteria());r.setUserConfirmMsg(old.getUserConfirmMsg());
                for(RequirementTask before:old.getTasks()) {
                    var t=new RequirementTask();copyMeta(before,t);t.setContent(before.getContent());t.setAcceptanceCriteria(before.getAcceptanceCriteria());t.setStatus(before.getStatus());
                    if(before.getResult()!=null) {
                        var a=before.getResult();var b=new AgentExecutionResult();b.setTaskId(a.getTaskId());b.setSuccess(a.isSuccess());b.setTokenCount(a.getTokenCount());b.setSummary(a.getSummary());
                        b.setErrorMessage(a.getErrorMessage());b.setConfirmationRequired(a.isConfirmationRequired());b.setConfirmationMessage(a.getConfirmationMessage());t.setResult(b);
                    }
                    r.getTasks().add(t);
                }
                p.getRequirements().add(r);
            }
            return p;
        }
        /** 复制绑定字段与元数据，保持每个角色独立。 */
        private static AgentBean agent(AgentBean from) { var copy=AbstractAgentExecutor.copyAgent(from);copyMeta(from,copy);return copy; }
        /** 复制数据库公共字段，深拷贝图时不增加版本。 */
        private static void copyMeta(DataBean from,DataBean to) { to.setId(from.getId());to.setVersion(from.getVersion());to.setCreationData(from.getCreationData());to.setLastupdateData(from.getLastupdateData());to.setDelFlg(from.isDelFlg()); }
    }
}
