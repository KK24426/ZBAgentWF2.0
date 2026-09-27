/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义项目 Agent 角色，并保留外部配置与提示词的小写名称。
 */
package com.kk24426.zbagentwf.common.project.bean;

/** 项目角色；Java 常量名称与稳定的外部配置名称分别保存。 */
public enum AgentRole {
    /** 规划模型 */
    PLANNING("planning"),
    /** 开发模型 */
    DEVELOPMENT("development"),
    /** 审核模型 */
    REVIEW("review");

    private final String configKey;

    AgentRole(String configKey) {
        this.configKey = configKey;
    }

    /** 返回角色配置键及对应提示词文件的名称，不随枚举常量改名。 */
    public String configKey() {
        return configKey;
    }

    /** 精确匹配已有小写配置；不裁剪、不忽略大小写或接受其它角色。 */
    public static AgentRole fromConfigKey(String configKey) {
        for (AgentRole role : values()) {
            if (role.configKey.equals(configKey)) return role;
        }
        throw new IllegalArgumentException("Agent 角色配置名称无效。");
    }
}
