/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：注册 Agent 聊天骨架，明确拒绝尚未接入的真实调用。
 */
package com.kk24426.zbagentwf.agent.chat;

import com.kk24426.zbagentwf.common.exception.AgentUnavailableException;
import com.kk24426.zbagentwf.user.AgentBase;
import com.kk24426.zbagentwf.user.chat.service.AgentChat;
import org.springframework.stereotype.Component;

/** 正式环境不模拟成功、不执行模型或外部进程；成功链路仅由测试替身验证。 */
@Component
public class AgentChatImpl extends AgentBase implements AgentChat {
    @Override
    public String reply(String message) {
        throw new AgentUnavailableException();
    }
}
