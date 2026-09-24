/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：承载一次 Agent 执行结束后的结果和确认事项。
 */
package com.kk24426.zbagentwf.common.agent.bean;

/** 承载一次 Agent 执行结束后的结果和确认事项。 普通属性原样存取，不自动执行业务校验。 */
public class AgentExecResult {
    /** 本次执行标识，与 exec 返回值一致，区别于规划任务 id。 */
    private String taskId;

    /** 执行成功时为 true；需要确认时必须为 false。 */
    private boolean success;

    /** 普通执行失败的原因；不得包含凭据，不应直接写入日志。 */
    private String errorMessage;

    /** 本次消耗的 token 数；未知时为 null。 */
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
