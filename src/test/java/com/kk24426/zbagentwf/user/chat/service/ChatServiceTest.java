/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：验证聊天服务输入边界、原样委派与生产未接入行为。
 */
package com.kk24426.zbagentwf.user.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.chat.AgentChatImpl;
import com.kk24426.zbagentwf.common.exception.AgentUnavailableException;
import org.junit.jupiter.api.Test;

class ChatServiceTest {
    @Test
    void delegatesUnchangedMessageAndReturnsAgentResult() {
        var agent = new ChatAgentFixture();
        var service = new ChatService(agent);
        String message = "  你好\n第二行  ";
        assertEquals(agent.result, service.reply(message));
        assertEquals(message, agent.message);
        assertEquals(1, agent.calls);
        assertEquals(agent.result, service.reply("🙂".repeat(2000)));
        assertEquals(4000, agent.message.length());
    }

    @Test
    void invalidMessagesNeverCallAgent() {
        var agent = new ChatAgentFixture();
        var service = new ChatService(agent);
        for (String message : new String[]{null, "", " \t\n", "　", "a".repeat(4001), "🙂".repeat(2001)}) {
            assertThrows(IllegalArgumentException.class, () -> service.reply(message));
        }
        assertEquals(0, agent.calls);
    }

    @Test
    void productionAgentRemainsExplicitlyUnavailableAndFailureIsNotSwallowed() {
        var service = new ChatService(new AgentChatImpl());
        var failure = assertThrows(AgentUnavailableException.class, () -> service.reply("普通隐私标记"));
        assertFalse(failure.getMessage().contains("普通隐私标记"));
        var agent = new ChatAgentFixture();
        agent.failure = new IllegalStateException("test failure");
        assertSame(agent.failure, assertThrows(IllegalStateException.class,
                () -> new ChatService(agent).reply("请求")));
    }
}
