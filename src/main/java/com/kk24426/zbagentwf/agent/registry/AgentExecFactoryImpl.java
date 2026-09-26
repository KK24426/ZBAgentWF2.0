/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按 Agent 实体隔离执行器并拥有共享资源生命周期。
 */
package com.kk24426.zbagentwf.agent.registry;

import com.kk24426.zbagentwf.agent.codex.*;
import com.kk24426.zbagentwf.agent.AgentExecutorImpl;
import com.kk24426.zbagentwf.agent.prompt.PromptCatalog;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.user.agent.userif.*;
import java.util.*;
import java.util.function.Function;

/** 缓存按实体身份保存；执行器由工厂关闭，业务调用方不单独关闭。 */
public final class AgentExecFactoryImpl implements AgentExecFactory, AutoCloseable {
    private final AgentCatalog catalog;
    private final ExecutionResources resources;
    private final Function<AgentDefinition, CodexClient> clients;
    private final Map<AgentBean, Entry> entries = new IdentityHashMap<>();
    private final PromptCatalog prompts;
    private final Map<AgentExecutor, CodexRequirementPlanner> planners = new IdentityHashMap<>();
    private boolean closed;

    public AgentExecFactoryImpl(AgentCatalog catalog, PromptCatalog prompts) {
        this(catalog, prompts, new ExecutionResources(), value -> new CodexClient(value.path(), value.model(), value.timeout()));
    }

    // 同包测试可替换进程边界；生产配置没有任意 command/args 或 shell 字符串入口。
    AgentExecFactoryImpl(AgentCatalog catalog, PromptCatalog prompts, ExecutionResources resources, Function<AgentDefinition, CodexClient> clients) {
        this.catalog = Objects.requireNonNull(catalog);
        this.prompts = Objects.requireNonNull(prompts);
        this.resources = Objects.requireNonNull(resources);
        this.clients = Objects.requireNonNull(clients);
    }

    @Override public synchronized AgentExecutor getExecutor(AgentBean agent) {
        ensureOpen();
        Binding binding = Binding.from(agent);
        AgentDefinition definition = catalog.require(binding.model());
        Entry entry = entries.get(agent);
        if (entry != null && !entry.binding.equals(binding)) {
            throw new IllegalArgumentException("已绑定 Agent 被修改，请创建新的 Agent 实体。");
        }
        if (entry == null) {
            // 先校验规则，再创建适配器；模型相同的不同实体仍分别拥有绑定快照。
            var defaults = prompts.require("default");
            var security = prompts.require("security");
            CodexClient client = clients.apply(definition);
            var executor = new CodexAgentExec(AgentExecutorImpl.copyAgent(agent), client, resources);
            executor.initializePrompts(defaults, security);
            var planner = new CodexRequirementPlanner(client, resources, executor);
            entry = new Entry(binding, executor, planner);
            entries.put(agent, entry);
            planners.put(executor, planner);
        }
        return entry.executor;
    }

    synchronized CodexRequirementPlanner plannerFor(AgentExecutor executor) {
        ensureOpen();
        // 按对象身份确认来自本工厂；相同模型的外部实例也不能借用本工厂的规划配置。
        CodexRequirementPlanner planner = planners.get(executor);
        if (planner == null) throw new IllegalArgumentException("项目规划 Agent 必须由当前工厂获取。");
        return planner;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Agent 工厂已关闭。");
    }

    /** ContextClosedEvent 调用；停止受理与进程中断不等待单个工作完成。 */
    public synchronized void beginShutdown() {
        if (closed) return;
        closed = true;
        resources.beginShutdown();
    }

    @Override public void close() {
        beginShutdown();
        // 等待时不持有工厂锁，避免完成回调重入工厂造成互相等待；资源层沿用原关闭截止时间。
        resources.close();
        synchronized (this) { entries.clear(); planners.clear(); }
    }

    private record Entry(Binding binding, CodexAgentExec executor, CodexRequirementPlanner planner) { }
    private record Binding(AgentDefinition.Key model, boolean hasRole, String role, boolean hasSkill, String skill) {
        private static Binding from(AgentBean bean) {
            var key = AgentDefinition.Key.from(bean);
            return new Binding(key, bean.getRolePrompt() != null,
                    bean.getRolePrompt() == null ? null : bean.getRolePrompt().getPrompt(), bean.getSkill() != null,
                    bean.getSkill() == null ? null : bean.getSkill().getSkillName());
        }
    }
}
