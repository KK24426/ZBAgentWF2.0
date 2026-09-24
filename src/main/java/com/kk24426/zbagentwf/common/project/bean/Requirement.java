/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存项目内的一条需求及其规划任务。
 */
package com.kk24426.zbagentwf.common.project.bean;

import java.util.ArrayList;
import java.util.List;

/** 保存项目内的一条需求及其规划任务。 普通属性原样存取，不自动执行业务校验。 */
public class Requirement {
    /** 用户原始需求。 */
    private String userContent;

    /** Agent 梳理后的需求理解。 */
    private String agentUnderstanding;

    /** 本条需求的验收标准。 */
    private String acceptanceCriteria;

    /** 属于本需求的 Task；默认列表由每条需求独立持有。 */
    private List<RequirementTask> tasks = new ArrayList<>();

    /** 需求规划中待用户确认的事项。 */
    private String userConfirmMsg;

    public String getUserContent() {
        return userContent;
    }

    public void setUserContent(String userContent) {
        this.userContent = userContent;
    }

    public String getAgentUnderstanding() {
        return agentUnderstanding;
    }

    public void setAgentUnderstanding(String agentUnderstanding) {
        this.agentUnderstanding = agentUnderstanding;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public List<RequirementTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<RequirementTask> tasks) {
        this.tasks = tasks;
    }

    public String getUserConfirmMsg() {
        return userConfirmMsg;
    }

    public void setUserConfirmMsg(String userConfirmMsg) {
        this.userConfirmMsg = userConfirmMsg;
    }
}
