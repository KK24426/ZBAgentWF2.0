/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：按项目对象绑定唯一协调器并管理共享执行资源。
 */
package com.kk24426.zbagentwf.agent.registry;
import com.kk24426.zbagentwf.agent.codex.*;
import com.kk24426.zbagentwf.agent.prompt.PromptCatalog;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.agent.persistence.ProjectDao;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.common.project.config.ProjectSettings;
import com.kk24426.zbagentwf.user.agent.api.*;
import java.util.*;
import java.util.function.Function;

/** canonical项目身份由领域实现维护；不同项目模型相同也不共享任务上下文。 */
public final class AgentExecutorFactoryImpl implements AgentExecutorFactory,AutoCloseable {
    private final AgentCatalog catalog;
    private final PromptCatalog prompts;
    private final ProjectSettings settings;
    private final ProjectDao dao;
    private final ExecutionResources resources;
    private final Function<AgentDefinition,CodexClient> clients;
    private final Map<Project,Entry> entries=new IdentityHashMap<>();
    private boolean closed;
    /** 工厂拥有共享资源；构造不发现模型或访问数据库。 */
    public AgentExecutorFactoryImpl(AgentCatalog catalog,PromptCatalog prompts,ProjectSettings settings,ProjectDao dao) {
        this(catalog,prompts,settings,dao,new ExecutionResources(), d -> new CodexClient(d.path(),d.model(),d.timeout()));
    }
    /** 同包测试可注入真实fixture子进程；无面向HTTP的任意命令入口。 */
    AgentExecutorFactoryImpl(AgentCatalog catalog,PromptCatalog prompts,ProjectSettings settings,ProjectDao dao,
            ExecutionResources resources,Function<AgentDefinition,CodexClient> clients) {
        this.catalog=Objects.requireNonNull(catalog); this.prompts=Objects.requireNonNull(prompts);
        this.settings=Objects.requireNonNull(settings); this.dao=Objects.requireNonNull(dao);
        this.resources=Objects.requireNonNull(resources); this.clients=Objects.requireNonNull(clients);
    }
    /** 校验三个角色后按项目身份缓存；模型、角色规则和推理程度改变后拒绝悄悄重绑。 */
    @Override public synchronized ProjectAgentExecutor getExecutor(Project project) {
        ensureOpen(); Objects.requireNonNull(project);
        List<Binding> binding=List.of(Binding.from(project.getPlanningAgent()),Binding.from(project.getDevelopmentAgent()),Binding.from(project.getReviewAgent()));
        var definitions=binding.stream().map(b -> catalog.require(b.model())).toList();
        Entry previous=entries.get(project);
        if (previous!=null) {
            if (!previous.projectId.equals(project.getProjectId()) || !previous.binding.equals(binding)) throw new IllegalArgumentException("已绑定项目的模型配置发生变化。");
            return previous.executor;
        }
        var defaults=prompts.require("default"); var security=prompts.require("security");
        CodexClient planning=clients.apply(definitions.get(0)); planning.initializeSecurity(security);
        CodexClient development=clients.apply(definitions.get(1)); development.initializeSecurity(security);
        var executor=new CodexAgentExecutor(project,planning,development,settings,dao,resources);
        executor.initializePrompts(defaults,security); entries.put(project,new Entry(Objects.requireNonNull(project.getProjectId()),binding,executor)); return executor;
    }
    /** 只取得当前工厂创建的项目协调器，供两阶段规划组合调用。 */
    synchronized CodexAgentExecutor coordinator(Project project) { return (CodexAgentExecutor)getExecutor(project); }
    /** 生命周期结束后不再解析配置或创建新的项目协调器。 */
    private void ensureOpen() { if (closed) throw new IllegalStateException("Agent工厂已关闭。"); }
    /** 关闭事件停止受理并中断所有项目、规划和审核，共用同一关闭预算。 */
    public synchronized void beginShutdown() { if (!closed) { closed=true; resources.beginShutdown(); } }
    /** 不持有工厂锁等待回调或资源释放，防止重入关闭死锁。 */
    @Override public void close() { beginShutdown(); resources.close(); synchronized(this) { entries.clear(); } }
    /** 生命周期与项目身份对应，不以模型相同合并实例。 */
    private record Entry(String projectId,List<Binding> binding,CodexAgentExecutor executor) { }
    /** 持久化ID和时间不属于模型绑定；文本规则及think是绑定快照的一部分。 */
    private record Binding(AgentDefinition.Key model,String role,String skill,String think) {
        /** 只读提取并拒绝不受支持附件，避免快照复制时丢失文件。 */
        private static Binding from(AgentBean bean) {
            Objects.requireNonNull(bean);
            if (bean.getRolePrompt()!=null) bean.getRolePrompt().requireTextOnly();
            return new Binding(AgentDefinition.Key.from(bean),bean.getRolePrompt()==null?null:bean.getRolePrompt().getPrompt(),
                    bean.getSkill()==null?null:bean.getSkill().getSkillName(),bean.getThink());
        }
    }
}
