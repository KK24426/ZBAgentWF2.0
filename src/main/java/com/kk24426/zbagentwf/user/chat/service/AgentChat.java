/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：定义单次用户消息到 Agent 文本回复的调用边界。
 */
package com.kk24426.zbagentwf.user.chat.service;

import com.kk24426.zbagentwf.common.exception.AgentUnavailableException;
import com.kk24426.zbagentwf.user.UserInterface;

/** 单次同步调用；真实 provider、会话和流式协议由后续任务确定。 */
public interface AgentChat extends UserInterface {
	/** 接收已经校验且未经改写的消息；未接入时抛出 {@link AgentUnavailableException}。 */
	String reply(String message);
}
