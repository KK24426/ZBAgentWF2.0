/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：承载一次成功 Agent 调用返回的文本结果。
 */
package com.kk24426.zbagentwf.common.chat;

/** 成功响应只包含 reply，不增加会话、状态或 provider 字段。 */
public record ChatResponse(String reply) {
}
