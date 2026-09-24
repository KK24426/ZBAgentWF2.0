/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：描述规划任务的执行状态，不实现状态流转。
 */
package com.kk24426.zbagentwf.common.project.bean;

/** 仅记录状态；调度、重试及状态转换由后续业务实现定义。 */
public enum TaskStatus {
    /** 尚未执行。 */
    PENDING,
    /** 正在执行。 */
    RUNNING,
    /** 执行成功。 */
    SUCCEEDED,
    /** 执行失败。 */
    FAILED,
    /** 本次执行已结束，需要用户确认后重新提交。 */
    NEEDS_CONFIRMATION
}
