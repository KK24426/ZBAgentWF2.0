/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：校验单次聊天消息并将原始内容交给 Agent 接口。
 */
package com.kk24426.zbagentwf.user.chat.service;

import com.kk24426.zbagentwf.user.UserService;
import org.springframework.stereotype.Service;

/** 用户侧编排，不依赖 HTTP、数据库或具体 Agent 实现。 */
@Service
public class ChatService extends UserService {
    private final AgentChat agentChat;

    public ChatService(AgentChat agentChat) {
        this.agentChat = agentChat;
    }

    /** 长度按 Java String 的 UTF-16 单元计数；仅校验，不 trim 或改写输入。 */
    public String reply(String message) {
        if (message == null || message.isBlank() || message.length() > 4000) {
            throw new IllegalArgumentException("消息不能为空，且长度不能超过 4000。");
        }
        return agentChat.reply(message);
    }
}
