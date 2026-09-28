/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：接收已受理的单任务或整批项目执行的最终结果。
 */
package com.kk24426.zbagentwf.user.agent.api;

import com.kk24426.zbagentwf.common.agent.model.AgentExecutionResult;

/** 最终结果回调；不承载过程事件，提交失败时不触发。 */
@FunctionalInterface
public interface AgentExecutionCallback {
    /**
     * 每次受理的单任务或整批执行结束后调用一次，需要确认也表示本次执行已结束。
     * execAllTasks不会为每个Task分别回调，批次ID与Task.result内的独立执行ID不同。
     *
     * @param result 携带受理返回的批次/单任务UUID taskId 的成功、失败或待确认结果
     */
    void onCompleted(AgentExecutionResult result);
}
