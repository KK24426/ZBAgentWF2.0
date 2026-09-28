/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：用短事务持久化完整项目聚合，并隔离结果不确定的写入。
 */
package com.kk24426.zbagentwf.agent.persistence;

import com.kk24426.zbagentwf.agent.persistence.mapper.ProjectMapper;
import com.kk24426.zbagentwf.common.*;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** 数据库是项目事实来源；DAO不调用模型，不尝试回滚项目文件。调用方负责同项目对象锁。 */
public class ProjectDao implements DataBeanDao<Project> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final ProjectMapper mapper;
    private final TransactionTemplate writes;
    private final TransactionTemplate reads;
    /** 仅隔离当前进程中写入结果不确定的对象；重新加载成功后才能继续写。 */
    private final Set<Project> uncertain = Collections.newSetFromMap(new WeakHashMap<>());

    /** null依赖表示未启用mysql，保留Web启动能力；所有项目操作明确报告未就绪。 */
    public ProjectDao(ProjectMapper mapper, PlatformTransactionManager transactions) {
        this.mapper = mapper;
        if (mapper == null || transactions == null) { writes = null; reads = null; }
        else {
            writes = new TransactionTemplate(transactions);
            writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            reads = new TransactionTemplate(transactions);
            reads.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            reads.setReadOnly(true);
            reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        }
    }

    /** 在目录和模型副作用之前检查数据库可用；不执行DDL或降级内存存储。 */
    public void requireAvailable() {
        if (mapper == null || writes == null) throw new AgentConfigurationUnavailableException();
        mapper.checkSchema();
    }

    /** 写入结果不确定时必须先重新读取数据库，不能用内存值自动重试。 */
    public void requireReliable(Project project) {
        synchronized (uncertain) {
            if (uncertain.contains(project)) throw new IllegalStateException("项目状态未能可靠保存，请重新查询；不要自动重跑任务。");
        }
    }

    /** 只有成功从数据库重载并发布到对象后才能清除不确定标志。 */
    public void reloaded(Project project) { synchronized (uncertain) { uncertain.remove(project); } }

    /** 按业务UUID完整读取四表快照；未知项目返回null，整个读取使用同一个一致读事务。 */
    public Project findByProjectId(String projectId) {
        return read(() -> { Long id = mapper.findProjectId(projectId); return id == null ? null : load(id); });
    }
    /** {@inheritDoc} 这里的ID是项目表Long主键。 */
    @Override public Project selectById(Long id) { return read(() -> load(id)); }
    /** 按项目主键顺序返回完整聚合，不把各子表的不同提交版本混在一起。 */
    @Override public List<Project> selectAll() {
        return read(() -> mapper.projectIds().stream().map(this::load).toList());
    }
    /** 一致读事务只覆盖SQL；不在事务中进入调用方锁或执行模型。 */
    private <T> T read(Supplier<T> action) {
        requireAvailable();
        return reads.execute(status -> action.get());
    }
    /** 新建项目连同角色、需求、任务一次提交；成功后回填所有实体ID。 */
    @Override public int insert(Project project) { return insertAll(List.of(project)); }
    /** 多项目写入共享一个短事务，任一失败整体回滚，不提前发布任何ID/version。 */
    @Override public int insertAll(List<Project> list) { return save(list, true); }
    /** 完整聚合按每行版本更新；冲突抛异常，不能静默覆盖。 */
    @Override public int update(Project project) { return update(List.of(project)); }
    /** 批量更新全部成功才发布实体元数据；模型执行不属于此事务。 */
    @Override public int update(List<Project> list) { return save(list, false); }

    /** 将数据库生成的元数据暂存在技术行和发布动作中，提交成功后才修改用户对象。 */
    private int save(List<Project> projects, boolean insert) {
        List<Project> selected = List.copyOf(projects);
        Set<Project> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Project project : selected) {
            if (!unique.add(project)) throw new IllegalArgumentException("重复项目。");
            requireReliable(project);
            if (insert != (project.getId() == null)) throw new IllegalArgumentException("项目持久化身份不符合操作。");
        }
        validateDistinctGraphs(selected);
        var publish = new ArrayList<Runnable>();
        try {
            // 可用性探测也可能发生在模型已完成之后；任何保存前连接故障都必须隔离内存候选。
            requireAvailable();
            writes.executeWithoutResult(status -> {
                for (Project project : selected) saveProject(project, insert, publish);
            });
            publish.forEach(Runnable::run);
            return selected.size();
        } catch (RuntimeException failure) {
            synchronized (uncertain) { uncertain.addAll(selected); }
            throw new IllegalStateException("项目状态未能可靠保存，请重新查询；不要自动重跑任务。", failure);
        }
    }

    /** 父子归属与既有顺序由完整聚合验证；不接受删除列表元素来隐式物理删除数据库行。 */
    private void saveProject(Project p, boolean insert, List<Runnable> publish) {
        Objects.requireNonNull(p.getProjectId());
        if (p.getPlanningAgent() == p.getDevelopmentAgent() || p.getPlanningAgent() == p.getReviewAgent()
                || p.getDevelopmentAgent() == p.getReviewAgent()) throw new IllegalArgumentException("各角色须使用独立快照。");
        if (!insert) {
            var stored = mapper.getProject(p.getId());
            if (stored == null || !p.getProjectId().equals(string(stored,"projectId"))
                    || !number(stored,"planningAgentId").equals(p.getPlanningAgent().getId())
                    || !number(stored,"developmentAgentId").equals(p.getDevelopmentAgent().getId())
                    || !number(stored,"reviewAgentId").equals(p.getReviewAgent().getId()))
                throw new IllegalArgumentException("项目标识或角色归属发生变化。");
        } else if (p.getPlanningAgent().getId()!=null || p.getDevelopmentAgent().getId()!=null || p.getReviewAgent().getId()!=null) {
            throw new IllegalArgumentException("新项目不能复用已保存角色行。");
        }
        Long planning = saveAgent(p.getPlanningAgent(), publish);
        Long development = saveAgent(p.getDevelopmentAgent(), publish);
        Long review = saveAgent(p.getReviewAgent(), publish);
        var row = row(p);
        row.put("projectId", p.getProjectId()); row.put("projectName", p.getProjectName());
        row.put("planningAgentId", planning); row.put("developmentAgentId", development); row.put("reviewAgentId", review);
        row.put("projectPrompt", text(p.getProjectPrompt()));
        persist(p, row, mapper::insertProject, mapper::updateProject, publish);
        Long projectId = id(row);
        List<Map<String,Object>> oldRequirements = insert ? List.of() : mapper.listStoredRequirements(projectId);
        ensureRetained(oldRequirements, p.getRequirements());
        int nextRequirementOrder = nextOrder(oldRequirements);
        for (Requirement r : p.getRequirements()) {
            Map<String,Object> before = stored(oldRequirements, r);
            // 已删除快照可留在调用方对象中；不更新、不复活，也不新增其子项。
            if (skipDeleted(before, r)) continue;
            int order = before == null ? nextRequirementOrder++ : ((Number)before.get("sortOrder")).intValue();
            var rr = row(r); rr.put("projectId", projectId); rr.put("sortOrder", order);
            rr.put("userContent", r.getUserContent()); rr.put("agentUnderstanding", r.getAgentUnderstanding());
            rr.put("acceptanceCriteria", r.getAcceptanceCriteria()); rr.put("userConfirmMsg", r.getUserConfirmMsg());
            boolean newRequirement = r.getId() == null;
            persist(r, rr, mapper::insertRequirement, mapper::updateRequirement, publish);
            Long requirementId = id(rr);
            var oldTasks = newRequirement ? List.<Map<String,Object>>of() : mapper.listStoredTasks(requirementId);
            ensureRetained(oldTasks, r.getTasks());
            int nextTaskOrder = nextOrder(oldTasks);
            for (RequirementTask t : r.getTasks()) {
                Map<String,Object> oldTask = stored(oldTasks, t);
                if (skipDeleted(oldTask, t)) continue;
                int taskOrder = oldTask == null ? nextTaskOrder++ : ((Number)oldTask.get("sortOrder")).intValue();
                var tr = row(t); tr.put("requirementId", requirementId); tr.put("sortOrder", taskOrder);
                tr.put("content", t.getContent()); tr.put("acceptanceCriteria", t.getAcceptanceCriteria());
                tr.put("status", Objects.requireNonNull(t.getStatus()).name());
                tr.put("resultJson", t.getResult() == null ? null : JSON.writeValueAsString(t.getResult()));
                persist(t, tr, mapper::insertTask, mapper::updateTask, publish);
            }
        }
    }

    /** 跨整个批次拒绝共享可变实体，避免提交后多个生成ID发布到同一对象。SQL前执行。 */
    private static void validateDistinctGraphs(List<Project> projects) {
        Set<DataBean> entities = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Project project : projects) {
            distinct(entities, project);
            for (AgentBean role : List.of(project.getPlanningAgent(),project.getDevelopmentAgent(),project.getReviewAgent())) {
                distinct(entities,role);
                if (role.isDelFlg()) throw new IllegalArgumentException("项目角色快照不能独立删除。");
            }
            for (Requirement requirement : Objects.requireNonNull(project.getRequirements())) {
                distinct(entities,requirement);
                for (RequirementTask task : Objects.requireNonNull(requirement.getTasks())) distinct(entities,task);
            }
        }
    }
    /** 每个保存实体只属于当前批次的一个位置；即使尚无主键也不能共享引用。 */
    private static void distinct(Set<DataBean> entities,DataBean bean) {
        if (!entities.add(Objects.requireNonNull(bean))) throw new IllegalArgumentException("持久化聚合不能共享实体对象。");
    }
    /** 已保存排序槽保持不变，新增项追加在全部行（含删除行）的最大槽之后。 */
    private static int nextOrder(List<Map<String,Object>> rows) {
        int maximum = rows.stream().mapToInt(row -> ((Number)row.get("sortOrder")).intValue()).max().orElse(-1);
        return Math.incrementExact(maximum);
    }
    /** 查询归属父实体的原始行；新实体返回null，外来主键明确拒绝。 */
    private static Map<String,Object> stored(List<Map<String,Object>> rows,DataBean bean) {
        if (bean.getId() == null) return null;
        return rows.stream().filter(row -> id(row).equals(bean.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("数据不属于当前父实体。"));
    }
    /** 已删除行不重复更新；本轮无复活业务，清除已保存删除标记明确拒绝。 */
    private static boolean skipDeleted(Map<String,Object> before,DataBean bean) {
        if (before == null || !deleted(before)) return false;
        if (!bean.isDelFlg()) throw new IllegalArgumentException("不能恢复已删除子项。");
        return true;
    }
    /** MySQL BOOLEAN在Map中可映射为Boolean或数值，统一读取删除标记。 */
    private static boolean deleted(Map<String,Object> row) {
        Object flag=row.get("delFlg"); return Boolean.TRUE.equals(flag) || flag instanceof Number n && n.intValue()!=0;
    }

    /** 保存每个角色独立的配置快照；不保存CLI路径、认证或运行时实例。 */
    private Long saveAgent(AgentBean agent, List<Runnable> publish) {
        Objects.requireNonNull(agent);
        var row = row(agent);
        row.put("brand", agent.getBrand()); row.put("name", agent.getName()); row.put("ver", agent.getVer());
        row.put("think", agent.getThink()); row.put("rolePrompt", text(agent.getRolePrompt()));
        row.put("skillName", agent.getSkill() == null ? null : agent.getSkill().getSkillName());
        persist(agent, row, mapper::insertAgent, mapper::updateAgent, publish);
        return id(row);
    }

    /** 生成写入技术行，旧创建时间保留，更新时间只属于本次候选提交。 */
    private static Map<String,Object> row(DataBean bean) {
        var values = new HashMap<String,Object>();
        values.put("id", bean.getId());
        values.put("creationData", bean.getCreationData() == null ? new Date() : bean.getCreationData());
        values.put("lastupdateData", new Date()); values.put("delFlg", bean.isDelFlg());
        values.put("version", bean.getVersion());
        return values;
    }

    /** 验证更新件数，随后登记提交后的发布动作；不在SQL执行时改变原对象。 */
    private static void persist(DataBean bean, Map<String,Object> row, ToIntFunction<Map<String,Object>> insert,
            ToIntFunction<Map<String,Object>> update, List<Runnable> publish) {
        boolean isNew = bean.getId() == null;
        int version = isNew ? 0 : Math.incrementExact(bean.getVersion());
        if (isNew) row.put("version", 0);
        int changed = isNew ? insert.applyAsInt(row) : update.applyAsInt(row);
        if (changed != 1 || row.get("id") == null) throw new IllegalStateException("数据版本冲突或写入件数异常。");
        Long id = id(row);
        publish.add(() -> {
            bean.setId(id); bean.setVersion(version);
            bean.setCreationData((Date) row.get("creationData")); bean.setLastupdateData((Date) row.get("lastupdateData"));
        });
    }

    /** 验证列表保留既有行和唯一ID；尚未定义删除/移动业务，不能从列表缺失推断删除。 */
    private static void ensureRetained(List<Map<String,Object>> existing, List<? extends DataBean> beans) {
        var ids = new HashSet<Long>();
        for (DataBean bean : Objects.requireNonNull(beans)) {
            if (bean.getId() != null && !ids.add(bean.getId())) throw new IllegalArgumentException("重复数据ID。");
        }
        for (var row : existing) if (!deleted(row) && !ids.contains(id(row))) throw new IllegalArgumentException("不能隐式删除已保存子项。");
    }
    /** 附件协议未支持时拒绝持久化，避免保存文本时静默丢失附件。 */
    private static String text(Prompt prompt) {
        if (prompt == null) return null;
        prompt.requireTextOnly(); return prompt.getPrompt();
    }
    private static Long id(Map<String,Object> row) { return ((Number) row.get("id")).longValue(); }
    private static String string(Map<String,Object> row, String key) { Object value = row.get(key); return value == null ? null : value.toString(); }
    private static Long number(Map<String,Object> row, String key) { return ((Number) row.get(key)).longValue(); }

    /** 已处于一致读事务，按保存顺序恢复整个项目；缺失角色或非法状态不能伪装成空数据。 */
    private Project load(Long projectId) {
        Map<String,Object> row = mapper.getProject(projectId);
        if (row == null) return null;
        Project p = new Project(string(row,"projectName"), new ArrayList<>(),
                agent(number(row,"planningAgentId")), agent(number(row,"developmentAgentId")), agent(number(row,"reviewAgentId")));
        meta(p,row); p.setProjectId(string(row,"projectId"));
        if (row.get("projectPrompt") != null) p.setProjectPrompt(new Prompt(string(row,"projectPrompt")));
        for (var rr : mapper.listRequirements(p.getId())) {
            var r = new Requirement(); meta(r,rr);
            r.setUserContent(string(rr,"userContent")); r.setAgentUnderstanding(string(rr,"agentUnderstanding"));
            r.setAcceptanceCriteria(string(rr,"acceptanceCriteria")); r.setUserConfirmMsg(string(rr,"userConfirmMsg"));
            for (var tr : mapper.listTasks(r.getId())) {
                var t = new RequirementTask(); meta(t,tr);
                t.setContent(string(tr,"content")); t.setAcceptanceCriteria(string(tr,"acceptanceCriteria"));
                t.setStatus(TaskStatus.valueOf(string(tr,"status")));
                if (tr.get("resultJson") != null) t.setResult(JSON.readValue(string(tr,"resultJson"), AgentExecutionResult.class));
                r.getTasks().add(t);
            }
            p.getRequirements().add(r);
        }
        return p;
    }

    /** 恢复角色数据快照；不可由数据库行触发模型发现、配置读取或执行。 */
    private AgentBean agent(Long id) {
        var row = Objects.requireNonNull(mapper.getAgent(id), "角色数据缺失。");
        var agent = new AgentBean(); meta(agent,row);
        agent.setBrand(string(row,"brand")); agent.setName(string(row,"name")); agent.setVer(string(row,"ver")); agent.setThink(string(row,"think"));
        if (row.get("rolePrompt") != null) agent.setRolePrompt(new Prompt(string(row,"rolePrompt")));
        if (row.get("skillName") != null) { var skill = new Skill(); skill.setSkillName(string(row,"skillName")); agent.setSkill(skill); }
        return agent;
    }

    /** 复制数据库公共元数据；读取不增加version。 */
    private static void meta(DataBean bean, Map<String,Object> row) {
        bean.setId(id(row)); bean.setVersion(((Number)row.get("version")).intValue());
        bean.setDelFlg(deleted(row));
        bean.setCreationData(date(row.get("creationData"))); bean.setLastupdateData(date(row.get("lastupdateData")));
    }

    /** Map映射中的DATETIME可由驱动返回LocalDateTime，按JVM时区恢复原Date语义。 */
    private static Date date(Object value) {
        if (value instanceof Date date) return new Date(date.getTime());
        if (value instanceof java.time.LocalDateTime time) return Date.from(time.atZone(java.time.ZoneId.systemDefault()).toInstant());
        throw new IllegalStateException("数据库时间类型无效。");
    }

    /** 保留canonical对象身份，发布已成功一致读的完整数据；调用方须持有target对象锁。 */
    public void refresh(Project target, Project fresh) {
        if (!target.getProjectId().equals(fresh.getProjectId())) throw new IllegalArgumentException("项目标识不一致。");
        target.setId(fresh.getId()); target.setVersion(fresh.getVersion()); target.setDelFlg(fresh.isDelFlg());
        target.setCreationData(fresh.getCreationData()); target.setLastupdateData(fresh.getLastupdateData());
        target.setProjectName(fresh.getProjectName()); target.setProjectPrompt(fresh.getProjectPrompt());
        target.setPlanningAgent(fresh.getPlanningAgent()); target.setDevelopmentAgent(fresh.getDevelopmentAgent());
        target.setReviewAgent(fresh.getReviewAgent()); target.setRequirements(fresh.getRequirements());
        reloaded(target);
    }
}
