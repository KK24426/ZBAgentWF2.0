/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：区分模型或规则尚未配置与执行中的内部错误。
 */
package com.kk24426.zbagentwf.common.exception;

/** 保留 IllegalStateException 父语义；HTTP 层使用固定本地化消息，不直接输出此异常。 */
public final class AgentConfigurationUnavailableException extends IllegalStateException {
    public AgentConfigurationUnavailableException() { super("Agent 模型、角色或必要规则未就绪。"); }
}
