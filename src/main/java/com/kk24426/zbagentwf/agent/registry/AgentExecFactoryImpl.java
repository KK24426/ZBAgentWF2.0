/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按模型三元组复用执行器并拥有共享资源生命周期。
 */
package com.kk24426.zbagentwf.agent.registry;

import com.kk24426.zbagentwf.agent.codex.*;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.user.agent.userif.*;
import java.util.*;
import java.util.function.Function;

/** 缓存范围受注册表约束；共享执行器由工厂关闭，业务调用方不单独关闭。 */
public final class AgentExecFactoryImpl implements AgentExecFactory, AutoCloseable {
    private final AgentCatalog catalog;
    private final ExecutionResources resources;
    private final Function<AgentDefinition, CodexClient> clients;
    private final Map<AgentDefinition.Key, Entry> entries = new HashMap<>();
    private final Map<AgentExecutor, CodexRequirementPlanner> planners = new IdentityHashMap<>();
    private boolean closed;

    public AgentExecFactoryImpl(AgentCatalog catalog) {
        this(catalog, new ExecutionResources(), value -> new CodexClient(value.path(), value.model(), value.timeout()));
    }

    // 同包测试可替换进程边界；生产配置没有任意 command/args 或 shell 字符串入口。
    AgentExecFactoryImpl(AgentCatalog catalog, ExecutionResources resources, Function<AgentDefinition, CodexClient> clients) {
        this.catalog = Objects.requireNonNull(catalog);
        this.resources = Objects.requireNonNull(resources);
        this.clients = Objects.requireNonNull(clients);
    }

    @Override public synchronized AgentExecutor getExecutor(AgentBean agent) {
        ensureOpen();
        var key = AgentDefinition.Key.from(agent);
        AgentDefinition definition = catalog.require(key);
        Entry entry = entries.get(key);
        if (entry == null) {
            CodexClient client = clients.apply(definition);
            var executor = new CodexAgentExec(definition.bean(), client, resources);
            var planner = new CodexRequirementPlanner(client, resources, executor);
            entry = new Entry(executor, planner);
            entries.put(key, entry);
            planners.put(executor, planner);
        }
        return entry.executor;
    }

    synchronized CodexRequirementPlanner plannerFor(AgentExecutor executor) {
        ensureOpen();
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
        resources.close();
        synchronized (this) { entries.clear(); planners.clear(); }
    }

    private record Entry(CodexAgentExec executor, CodexRequirementPlanner planner) { }
}
