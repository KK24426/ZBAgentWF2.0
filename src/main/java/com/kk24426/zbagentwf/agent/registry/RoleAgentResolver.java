/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：将任意已选择模型与角色规则绑定，缺省选择由显式配置提供。
 */
package com.kk24426.zbagentwf.agent.registry;

import com.kk24426.zbagentwf.agent.runtime.AbstractAgentExecutor;
import com.kk24426.zbagentwf.agent.prompt.PromptCatalog;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.common.project.model.AgentRole;
import java.util.Map;
import java.util.Objects;

/** 模型与角色独立；每次绑定生成新的实体，不改变调用方的 Bean。 */
public final class RoleAgentResolver {
    private final Map<String, Selection> defaults;
    private final PromptCatalog prompts;

    /**
     * 保存角色选择快照并验证角色名称；允许模型字段尚未配置，实际使用时再检查完整性，不读取规则文件。
     */
    public RoleAgentResolver(Map<String, Selection> defaults, PromptCatalog prompts) {
        this.defaults = Map.copyOf(defaults);
        this.prompts = Objects.requireNonNull(prompts);
        for (String role : defaults.keySet()) AgentRole.fromConfigKey(role);
    }

    /**
     * 取得指定角色的完整默认三元组并绑定规则；缺失或不完整抛配置不可用异常，不选择列表首项。
     */
    public AgentBean defaultFor(AgentRole role) {
        var key = defaults.get(role.configKey());
        if (key == null || blank(key.brand()) || blank(key.name()) || blank(key.ver())) throw new AgentConfigurationUnavailableException();
        return bind(key.bean(), role);
    }

    /** 将null和纯空白都视为默认模型字段尚未配置，不替换实际字段值。 */
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    /** 缺省角色字段允许尚未配置，真正创建时才校验，不影响仅使用 Web。 */
    public record Selection(String brand, String name, String ver) {
        /** 将已选择的三元组原样复制到新Bean；完整性由defaultFor在转换前校验。 */
        private AgentBean bean() {
            var value = new AgentBean(); value.setBrand(brand); value.setName(name); value.setVer(ver); return value;
        }
    }

    /**
     * 复制已选择模型并补充角色规则，不改变调用方Bean或检查模型可执行性。
     * 显式rolePrompt优先；仅对象为null才使用角色文件，显式空文本不会静默回退。
     * @return 每次独立的新模型实体，供工厂按身份绑定
     * @throws com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException 角色规则缺失或空白
     */
    public AgentBean bind(AgentBean selected, AgentRole role) {
        var bound = AbstractAgentExecutor.copyAgent(Objects.requireNonNull(selected, "必须指定角色模型。"));
        if (bound.getRolePrompt() == null) bound.setRolePrompt(prompts.require(role.configKey()));
        if (bound.getRolePrompt().getPrompt() == null || bound.getRolePrompt().getPrompt().isBlank()) {
            throw new AgentConfigurationUnavailableException();
        }
        return bound;
    }
}
