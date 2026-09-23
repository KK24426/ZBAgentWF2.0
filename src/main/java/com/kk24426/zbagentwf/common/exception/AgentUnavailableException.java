/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：共享 Agent 不可用异常，不携带用户输入或 provider 详情。
 */
package com.kk24426.zbagentwf.common.exception;

/** Agent 实现抛出、调用层捕获的共享技术异常；Web 层映射为服务不可用。 */
public final class AgentUnavailableException extends RuntimeException {
    public AgentUnavailableException() {
        super("Agent 尚未接入，暂时无法生成回复。");
    }
}
