/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：关联需求任务与其负责的变更计划。
 */
package com.kk24426.zbagentwf.common.project.model;

import com.kk24426.zbagentwf.common.DataBean;

/**
 * 保存任务到计划项的关联数据；一个任务可关联同一模块的多项计划，应用级任务单独配置。
 * 普通属性原样存取，无数据库、文件、模型调用或状态转换。
 */
public class TaskChangePlanLink extends DataBean {

    /** 所属 Project.id 数据库主键；不是 Project.projectId 的字符串 UUID。 */
    private Long projectId;

    /** 所属 Requirement.id 数据库主键，必须属于同一项目。 */
    private Long requirementId;

    /** 执行任务的 RequirementTask.id 数据库主键，必须属于同一需求。 */
    private Long requirementTaskId;

    /** 关联 RequirementChangePlanItem.id 数据库主键。 */
    private Long requirementChangePlanItemId;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public Long getRequirementTaskId() {
        return requirementTaskId;
    }

    public void setRequirementTaskId(Long requirementTaskId) {
        this.requirementTaskId = requirementTaskId;
    }

    public Long getRequirementChangePlanItemId() {
        return requirementChangePlanItemId;
    }

    public void setRequirementChangePlanItemId(Long requirementChangePlanItemId) {
        this.requirementChangePlanItemId = requirementChangePlanItemId;
    }
}
