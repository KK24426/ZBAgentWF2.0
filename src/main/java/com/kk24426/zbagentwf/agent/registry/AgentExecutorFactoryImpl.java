/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按 Agent 实体隔离执行器并拥有共享资源生命周期。
 */
package com.kk24426.zbagentwf.agent.registry;

import com.kk24426.zbagentwf.agent.codex.*;
import com.kk24426.zbagentwf.agent.runtime.AbstractAgentExecutor;
import com.kk24426.zbagentwf.agent.prompt.PromptCatalog;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.user.agent.api.*;
import java.util.*;
import java.util.function.Function;

/** 缓存按实体身份保存；执行器由工厂关闭，业务调用方不单独关闭。 */
public final class AgentExecutorFactoryImpl implements AgentExecutorFactory, AutoCloseable {
    private final AgentCatalog catalog;
    private final ExecutionResources resources;
    private final Function<AgentDefinition, CodexClient> clients;
    /**
     * 按调用方Bean对象身份缓存绑定；同值不同对象分别创建执行器，不能按equals合并。
     */
    private final Map<AgentBean, Entry> entries = new IdentityHashMap<>();
    private final PromptCatalog prompts;
    /**
     * 只登记本工厂创建的执行器身份，防止外部同模型实例借用规划规则。
     */
    private final Map<AgentExecutor, CodexRequirementPlanner> planners = new IdentityHashMap<>();
    private boolean closed;

    /**
     * 建立工厂自有共享资源作用域；执行器按需创建，工厂统一关闭，构造不运行模型。
     */
    public AgentExecutorFactoryImpl(AgentCatalog catalog, PromptCatalog prompts) {
        this(catalog, prompts, new ExecutionResources(), value -> new CodexClient(value.path(), value.model(), value.timeout()));
    }

    // 同包测试可替换进程边界；生产配置没有任意 command/args 或 shell 字符串入口。
    /** 注入可替换的进程客户端创建器及共享资源；工厂关闭时仍统一关闭该资源，构造不调用创建器。 */
    AgentExecutorFactoryImpl(AgentCatalog catalog, PromptCatalog prompts, ExecutionResources resources, Function<AgentDefinition, CodexClient> clients) {
        this.catalog = Objects.requireNonNull(catalog);
        this.prompts = Objects.requireNonNull(prompts);
        this.resources = Objects.requireNonNull(resources);
        this.clients = Objects.requireNonNull(clients);
    }

    /**
     * 按实体身份获取或创建执行器，首次创建时校验模型可用性及必要提示词并保存深拷贝。
     * 同一实体的绑定字段或嵌套规则改变后拒绝复用；同值的不同实体不会共用执行器。
     * @param agent 调用方持有的模型及角色规则实体
     * @return 由本工厂管理生命周期的实例，业务调用方不单独关闭
     * @throws IllegalArgumentException 模型身份无效或已绑定实体被修改
     * @throws IllegalStateException 工厂关闭、模型不可用或必要规则未就绪
     */
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
            var executor = new CodexAgentExecutor(AbstractAgentExecutor.copyAgent(agent), client, resources);
            executor.initializePrompts(defaults, security);
            var planner = new CodexRequirementPlanner(client, resources, executor);
            entry = new Entry(binding, executor, planner);
            entries.put(agent, entry);
            planners.put(executor, planner);
        }
        return entry.executor;
    }

    /**
     * 按执行器身份取得本工厂创建的只读规划器；关闭或跨工厂/自定义实例绑定均明确失败。
     */
    synchronized CodexRequirementPlanner plannerFor(AgentExecutor executor) {
        ensureOpen();
        // 按对象身份确认来自本工厂；相同模型的外部实例也不能借用本工厂的规划配置。
        CodexRequirementPlanner planner = planners.get(executor);
        if (planner == null) throw new IllegalArgumentException("项目规划 Agent 必须由当前工厂获取。");
        return planner;
    }

    /** 调用者持有工厂锁时检查关闭标记；关闭后拒绝取得或创建绑定。 */
    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Agent 工厂已关闭。");
    }

    /** ContextClosedEvent 调用；停止受理与进程中断不等待单个工作完成。 */
    public synchronized void beginShutdown() {
        if (closed) return;
        closed = true;
        resources.beginShutdown();
    }

    /**
     * 先停止受理，再在不持有工厂锁时按资源截止时间等待，最后释放实例索引；重复关闭沿用原截止时间。
     */
    @Override public void close() {
        beginShutdown();
        // 等待时不持有工厂锁，避免完成回调重入工厂造成互相等待；资源层沿用原关闭截止时间。
        resources.close();
        synchronized (this) { entries.clear(); planners.clear(); }
    }

    /**
     * 同一实体的原始绑定快照、执行器和专用规划器，保存至工厂关闭。
     */
    private record Entry(Binding binding, CodexAgentExecutor executor, CodexRequirementPlanner planner) { }
    /**
     * 按当前工厂支持的三元组、角色文本和Skill名称检测绑定后变更；空对象与空文本分别记录。
     */
    private record Binding(AgentDefinition.Key model, boolean hasRole, String role, boolean hasSkill, String skill) {
        /**
         * 只提取绑定校验使用的值，不保留可变嵌套对象；后续比较用于拒绝已绑定实体的重配置。
         */
        private static Binding from(AgentBean bean) {
            var key = AgentDefinition.Key.from(bean);
            return new Binding(key, bean.getRolePrompt() != null,
                    bean.getRolePrompt() == null ? null : bean.getRolePrompt().getPrompt(), bean.getSkill() != null,
                    bean.getSkill() == null ? null : bean.getSkill().getSkillName());
        }
    }
}
