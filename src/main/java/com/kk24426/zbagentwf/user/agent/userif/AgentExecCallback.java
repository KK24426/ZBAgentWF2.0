/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：接收已受理的单次 Agent 执行的最终结果。
 */
package com.kk24426.zbagentwf.user.agent.userif;

import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;

/** 最终结果回调；不承载过程事件，提交失败时不触发。 */
@FunctionalInterface
public interface AgentExecCallback {
    /**
     * 每次受理的执行结束后调用一次，需要确认也表示本次执行已结束。
     *
     * @param result 携带本次执行 taskId 的成功、失败或待确认结果
     */
    void onCompleted(AgentExecResult result);
}
