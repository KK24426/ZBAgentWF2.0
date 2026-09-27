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

    public RoleAgentResolver(Map<String, Selection> defaults, PromptCatalog prompts) {
        this.defaults = Map.copyOf(defaults);
        this.prompts = Objects.requireNonNull(prompts);
        for (String role : defaults.keySet()) AgentRole.fromConfigKey(role);
    }

    public AgentBean defaultFor(AgentRole role) {
        var key = defaults.get(role.configKey());
        if (key == null || blank(key.brand()) || blank(key.name()) || blank(key.ver())) throw new AgentConfigurationUnavailableException();
        return bind(key.bean(), role);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    /** 缺省角色字段允许尚未配置，真正创建时才校验，不影响仅使用 Web。 */
    public record Selection(String brand, String name, String ver) {
        private AgentBean bean() {
            var value = new AgentBean(); value.setBrand(brand); value.setName(name); value.setVer(ver); return value;
        }
    }

    public AgentBean bind(AgentBean selected, AgentRole role) {
        var bound = AbstractAgentExecutor.copyAgent(Objects.requireNonNull(selected, "必须指定角色模型。"));
        if (bound.getRolePrompt() == null) bound.setRolePrompt(prompts.require(role.configKey()));
        if (bound.getRolePrompt().getPrompt() == null || bound.getRolePrompt().getPrompt().isBlank()) {
            throw new AgentConfigurationUnavailableException();
        }
        return bound;
    }
}
