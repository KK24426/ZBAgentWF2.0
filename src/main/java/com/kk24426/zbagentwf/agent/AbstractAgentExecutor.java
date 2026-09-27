/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：保存 Agent 绑定快照并在显式初始化后组合通用与角色规则。
 */
package com.kk24426.zbagentwf.agent;

import com.kk24426.zbagentwf.common.agent.bean.*;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.common.project.bean.Project;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecutor;
import java.util.Objects;

/** 模型通用父类；构造和 getter 均不读取文件、不执行模型。 */
public abstract class AbstractAgentExecutor extends AgentExecutor {
    private volatile Rules rules;

    protected AbstractAgentExecutor(AgentBean agent) { this(agent, null, null); }
    protected AbstractAgentExecutor(AgentBean agent, Prompt userPrompt, Prompt projectPrompt) {
        super(copyAgent(agent), copyPrompt(userPrompt), copyPrompt(projectPrompt));
    }

    /** 配置由装配方提前读取；这里只保存不可变文本，禁止已绑定实例重新配置。 */
    public final synchronized void initializePrompts(Prompt defaults, Prompt security) {
        if (rules != null) throw new IllegalStateException("执行器规则已经初始化。");
        rules = new Rules(required(defaults), required(security));
    }

    @Override protected final Prompt getDefaultPrompt() { return prompt(requireRules().defaults()); }
    @Override protected final Prompt getSecurityPrompt() { return prompt(requireRules().security()); }

    /** 在受理前生成本次快照；项目追加规则只影响之后的调用。 */
    public final String instructionsFor(Project project) {
        Rules snapshot = requireRules();
        synchronized (Objects.requireNonNull(project)) {
            Prompt current = project.getProjectPrompt() == null ? getProjectPrompt() : project.getProjectPrompt();
            return instructions(prompt(snapshot.defaults()), prompt(snapshot.security()),
                    getAgent().getRolePrompt(), getUserPrompt(), current);
        }
    }

    /** 规划与执行采用相同的分段格式；必要规则为空时在任何进程启动前拒绝。 */
    public static String instructions(Prompt defaults, Prompt security, Prompt role, Prompt user, Prompt project) {
        var text = new StringBuilder();
        section(text, "安全规则", required(security));
        section(text, "通用规则", required(defaults));
        section(text, "角色规则", value(role));
        section(text, "用户规则", value(user));
        section(text, "项目规则", value(project));
        return text.toString();
    }

    private Rules requireRules() {
        Rules value = rules;
        if (value == null) throw new AgentConfigurationUnavailableException();
        return value;
    }
    private static String required(Prompt prompt) {
        String value = value(prompt);
        if (value == null || value.isBlank()) throw new AgentConfigurationUnavailableException();
        return value;
    }
    private static void section(StringBuilder text, String name, String value) {
        if (value != null && !value.isBlank()) text.append("【").append(name).append("】\n").append(value).append("\n");
    }
    private static String value(Prompt prompt) { return prompt == null ? null : prompt.getPrompt(); }
    public static Prompt prompt(String value) { var p = new Prompt(); p.setPrompt(value); return p; }
    public static Prompt copyPrompt(Prompt value) { return value == null ? null : prompt(value.getPrompt()); }
    public static AgentBean copyAgent(AgentBean value) {
        Objects.requireNonNull(value, "Agent 不能为空。");
        var copy = new AgentBean();
        copy.setBrand(value.getBrand()); copy.setName(value.getName()); copy.setVer(value.getVer());
        copy.setRolePrompt(copyPrompt(value.getRolePrompt()));
        if (value.getSkill() != null) { var skill = new Skill(); skill.setSkillName(value.getSkill().getSkillName()); copy.setSkill(skill); }
        return copy;
    }
    private record Rules(String defaults, String security) { }
}
