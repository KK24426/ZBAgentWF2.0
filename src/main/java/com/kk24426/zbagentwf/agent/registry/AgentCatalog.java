/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按版本维护配置模型及本机可执行性快照。
 */
package com.kk24426.zbagentwf.agent.registry;

import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.user.agent.userif.AgentBase;
import java.nio.file.Files;
import java.util.*;

/** 不查询账号模型、不执行 CLI；修改配置后重启，刷新只更新本机文件可用性。 */
public final class AgentCatalog extends AgentBase {
    private final Map<AgentDefinition.Key, AgentDefinition> definitions;
    // 配置本身不可变；刷新构造新快照后一次发布，查询不会读到清空或填充中的中间状态。
    private volatile Map<AgentDefinition.Key, AgentDefinition> available = Map.of();

    public AgentCatalog(List<AgentDefinition> definitions) {
        var configured = new LinkedHashMap<AgentDefinition.Key, AgentDefinition>();
        for (AgentDefinition definition : definitions) {
            // 禁用配置也占据模型身份，不能容忍重复后再按可用性偷偷选择其中一项。
            if (configured.putIfAbsent(definition.key(), definition) != null) {
                throw new IllegalArgumentException("Agent 配置包含重复的 brand/name/ver。");
            }
        }
        this.definitions = Collections.unmodifiableMap(configured);
    }

    /** Spring 显式初始化；允许空目录以保留仅使用 Web 的启动方式。 */
    public void initialize() { updateAvailability(); }

    private synchronized void updateAvailability() {
        var next = new LinkedHashMap<AgentDefinition.Key, AgentDefinition>();
        for (var entry : definitions.entrySet()) {
            var value = entry.getValue();
            // 根目录没有文件名；先排除禁用项和非普通文件，再检查本机扩展名。
            if (!value.enabled() || !Files.isRegularFile(value.path()) || !Files.isExecutable(value.path())) continue;
            // Windows 只接受原生程序，避免为 .cmd/.bat 引入隐式 shell 启动与转义规则。
            boolean nativeExecutable = !System.getProperty("os.name").startsWith("Windows")
                    || value.path().getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe");
            if (nativeExecutable) {
                next.put(entry.getKey(), value);
            }
        }
        available = Collections.unmodifiableMap(next);
    }

    @Override public List<AgentBean> getActiveAgent() {
        // 单次查询使用同一快照，并返回新 Bean；调用方修改元数据不会污染后续查询。
        var snapshot = available;
        if (snapshot.isEmpty()) throw new IllegalStateException("没有已配置且本机可用的 Agent。");
        return snapshot.values().stream().map(AgentDefinition::bean).toList();
    }

    @Override public AgentBean getActiveAgent(String brand, String name, String ver) {
        return require(new AgentDefinition.Key(brand, name, ver)).bean();
    }

    @Override public List<AgentBean> refreshAgentList() {
        // 即使刷新结果为空，也先替换旧快照，再明确报错，不能继续返回已失效模型。
        updateAvailability();
        return getActiveAgent();
    }

    AgentDefinition require(AgentDefinition.Key key) {
        // 精确匹配失败就报告不可用，不按品牌、名称或旧版本降级选择其它注册。
        AgentDefinition value = available.get(key);
        if (value == null) throw new IllegalStateException("指定 Agent 未注册、未启用或本机不可用。");
        return value;
    }
}
