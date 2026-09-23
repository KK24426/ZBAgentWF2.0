/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：仅供测试的 Agent 替身，记录调用并提供可控回复或失败。
 */
package com.kk24426.zbagentwf.user.chat.service;

/** 无组件注解，不参与正式扫描或 JAR 打包。 */
public final class ChatAgentFixture implements AgentChat {
    public int calls;
    public String message;
    public String result = "测试回复：你好\n第二行";
    public RuntimeException failure;

    @Override
    public String reply(String message) {
        calls++;
        this.message = message;
        if (failure != null) throw failure;
        return result;
    }
}
