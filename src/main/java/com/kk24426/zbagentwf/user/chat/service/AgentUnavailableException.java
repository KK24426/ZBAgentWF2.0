/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：明确表达当前尚未接入真实 Agent，不携带用户输入或 provider 详情。
 */
package com.kk24426.zbagentwf.user.chat.service;

/** 固定错误语义，供 Web 层映射为服务不可用。 */
public final class AgentUnavailableException extends RuntimeException {
    public AgentUnavailableException() {
        super("Agent 尚未接入，暂时无法生成回复。");
    }
}
