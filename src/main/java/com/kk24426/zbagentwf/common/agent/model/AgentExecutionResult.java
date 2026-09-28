/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：承载任务执行或项目批次的最终结果和确认事项。
 */
package com.kk24426.zbagentwf.common.agent.model;

/** 承载任务执行或项目批次的最终结果和确认事项。 普通属性原样存取，不自动执行业务校验。 */
public class AgentExecutionResult {
    /** 回调结果为execAllTasks批次UUID或execTasks单次UUID，与受理返回值一致；Task.result保存该任务独立执行UUID，均非数据库Task.id。 */
    private String taskId;

    /** 执行成功时为 true；需要确认时必须为 false。 */
    private boolean success;

    /** 普通执行失败的原因；不得包含凭据，不应直接写入日志。 */
    private String errorMessage;

    /** 本次审核及业务消耗的token数，批次结果汇总本批次；未知或溢出时为null。 */
    private Long tokenCount;

    /** 本次执行的汇报或总结。 */
    private String summary;

    /** 本次已结束且需要用户确认；确认后重新提交，获得新的执行标识。 */
    private boolean confirmationRequired;

    /** 需要用户确认的具体内容。 */
    private String confirmationMessage;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Long tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public String getConfirmationMessage() {
        return confirmationMessage;
    }

    public void setConfirmationMessage(String confirmationMessage) {
        this.confirmationMessage = confirmationMessage;
    }
}
