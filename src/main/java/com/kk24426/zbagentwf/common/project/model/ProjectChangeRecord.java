/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：记录一次已确认实际修改及其需求、任务和前后快照。
 */
package com.kk24426.zbagentwf.common.project.model;

import com.kk24426.zbagentwf.common.DataBean;

/**
 * 实际修改履历，与规划意图分离。保存后只追加，不提供修改或删除历史的领域入口。
 * 本类仍是普通可变 Bean；不可修改已保存历史的约束由后续实现负责。
 * 归属路径必须已解析为真实 ID；应用目标的模块/功能 ID、模块目标的功能 ID 为空。
 * 普通属性原样存取，无数据库、文件、模型调用或状态转换。
 */
public class ProjectChangeRecord extends DataBean {

    /** 所属 Project.id 数据库主键；不是 Project.projectId 的字符串 UUID。 */
    private Long projectId;

    /** 所属 Requirement.id 数据库主键，必须属于同一项目。 */
    private Long requirementId;

    /** 执行任务的 RequirementTask.id 数据库主键，必须属于同一需求。 */
    private Long requirementTaskId;

    /** 关联 RequirementChangePlanItem.id 数据库主键。 */
    private Long requirementChangePlanItemId;

    /** 单次任务执行 UUID；不是任务数据库主键，也不是外键。 */
    private String executionId;

    /** 实际修改的对象层级。 */
    private ProjectElementType targetType;

    /** 本次实际操作；首次创建后再次修改同一对象时为 UPDATE，即使原计划为 CREATE。 */
    private ProjectChangeOperation operation;

    /** 真实归属路径的 ProjectApplication.id。 */
    private Long projectApplicationId;

    /** 真实归属路径的 ProjectModule.id；应用级履历为空。 */
    private Long projectModuleId;

    /** 实际目标 ProjectFunction.id；应用或模块级履历为空。 */
    private Long projectFunctionId;

    /** 已经发生且得到确认的实际修改说明。 */
    private String changeDescription;

    /** 修改前完整持久化快照；实际 CREATE 时为空，UPDATE 时必填。 */
    private ProjectElementSnapshot beforeSnapshot;

    /** 修改后完整持久化快照；身份、归属和版本均来自实际保存结果。 */
    private ProjectElementSnapshot afterSnapshot;

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

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public ProjectElementType getTargetType() {
        return targetType;
    }

    public void setTargetType(ProjectElementType targetType) {
        this.targetType = targetType;
    }

    public ProjectChangeOperation getOperation() {
        return operation;
    }

    public void setOperation(ProjectChangeOperation operation) {
        this.operation = operation;
    }

    public Long getProjectApplicationId() {
        return projectApplicationId;
    }

    public void setProjectApplicationId(Long projectApplicationId) {
        this.projectApplicationId = projectApplicationId;
    }

    public Long getProjectModuleId() {
        return projectModuleId;
    }

    public void setProjectModuleId(Long projectModuleId) {
        this.projectModuleId = projectModuleId;
    }

    public Long getProjectFunctionId() {
        return projectFunctionId;
    }

    public void setProjectFunctionId(Long projectFunctionId) {
        this.projectFunctionId = projectFunctionId;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public ProjectElementSnapshot getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public void setBeforeSnapshot(ProjectElementSnapshot beforeSnapshot) {
        this.beforeSnapshot = beforeSnapshot;
    }

    public ProjectElementSnapshot getAfterSnapshot() {
        return afterSnapshot;
    }

    public void setAfterSnapshot(ProjectElementSnapshot afterSnapshot) {
        this.afterSnapshot = afterSnapshot;
    }
}
