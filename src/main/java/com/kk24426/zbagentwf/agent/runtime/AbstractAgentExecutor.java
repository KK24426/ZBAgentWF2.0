/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：保存 Agent 绑定快照并在显式初始化后组合通用与角色规则。
 */
package com.kk24426.zbagentwf.agent.runtime;

import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.user.agent.api.ProjectAgentExecutor;
import java.util.Objects;

/** 模型通用父类；构造和 getter 均不读取文件、不执行模型。 */
public abstract class AbstractAgentExecutor extends ProjectAgentExecutor {
    private volatile Rules rules;

    /** 保存项目和可选用户规则，不读取文件或调用模型。 */
    protected AbstractAgentExecutor(Project project) { super(project); }

    /** 配置由装配方提前读取；这里只保存不可变文本，禁止已绑定实例重新配置。 */
    public final synchronized void initializePrompts(Prompt defaults, Prompt security) {
        if (rules != null) throw new IllegalStateException("执行器规则已经初始化。");
        rules = new Rules(required(defaults), required(security));
    }

    /** 返回已初始化通用规则的新Prompt，隔离调用方修改；未初始化时抛配置不可用异常。 */
    @Override protected final Prompt getDefaultPrompt() { return prompt(requireRules().defaults()); }
    /** 返回已初始化安全规则的新Prompt，隔离调用方修改；未初始化时抛配置不可用异常。 */
    @Override protected final Prompt getSecurityPrompt() { return prompt(requireRules().security()); }

    /** 以指定角色快照和项目当前规则组合本次输入，不读取文件或调用模型。 */
    public final String instructionsFor(AgentBean role) {
        Rules snapshot = requireRules();
        synchronized (getProject()) {
            return instructions(prompt(snapshot.defaults()), prompt(snapshot.security()),
                    role.getRolePrompt(), getUserPrompt(), getProject().getProjectPrompt());
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

    /**
     * 取得已发布的必要规则快照；未初始化时明确失败，不能以空规则开始规划或执行。
     */
    private Rules requireRules() {
        Rules value = rules;
        if (value == null) throw new AgentConfigurationUnavailableException();
        return value;
    }
    /** 读取必要提示词的非空白原文；缺失时报告配置未就绪，不生成默认规则。 */
    private static String required(Prompt prompt) {
        String value = value(prompt);
        if (value == null || value.isBlank()) throw new AgentConfigurationUnavailableException();
        return value;
    }
    /** 仅为非空白规则追加有名称的段落；保持规则原文，缺省可选段落不写占位文本。 */
    private static void section(StringBuilder text, String name, String value) {
        if (value != null && !value.isBlank()) text.append("【").append(name).append("】\n").append(value).append("\n");
    }
    /** 可选规则允许null，但非空附件必须在组装时明确拒绝。 */
    private static String value(Prompt prompt) { if (prompt == null) return null; prompt.requireTextOnly(); return prompt.getPrompt(); }
    /** 将原文包裹为新Prompt，可保留null；必要规则的校验由使用入口完成。 */
    public static Prompt prompt(String value) { return new Prompt(value); }
    /**
     * 复制不可变提示词及防御复制的附件数组；null保持为null，不执行附件解码。
     */
    public static Prompt copyPrompt(Prompt value) { return value == null ? null : new Prompt(value.getPrompt(), value.getFile()); }
    /**
     * 复制当前绑定使用的模型三元组、角色提示词和 Skill 名称，隔离可变嵌套对象。
     * 这是执行器绑定快照，不是 AgentBean 所有字段的通用克隆；同时保留用户定义的 think 推理程度，不自行推断CLI参数。
     * @param value 非空模型实体
     * @return 用于绑定的新实体，未指定的可选对象保持 null
     */
    public static AgentBean copyAgent(AgentBean value) {
        Objects.requireNonNull(value, "Agent 不能为空。");
        var copy = new AgentBean();
        copy.setThink(value.getThink());
        copy.setBrand(value.getBrand()); copy.setName(value.getName()); copy.setVer(value.getVer());
        copy.setRolePrompt(copyPrompt(value.getRolePrompt()));
        if (value.getSkill() != null) { var skill = new Skill(); skill.setSkillName(value.getSkill().getSkillName()); copy.setSkill(skill); }
        return copy;
    }
    /** 一次性发布的必要规则文本快照，两个字段均已通过非空白校验。 */
    private record Rules(String defaults, String security) { }
}
