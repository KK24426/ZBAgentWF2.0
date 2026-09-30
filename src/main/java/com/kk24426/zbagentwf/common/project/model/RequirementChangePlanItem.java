/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存需求预计修改的对象及修改边界。
 */
package com.kk24426.zbagentwf.common.project.model;

import com.kk24426.zbagentwf.common.DataBean;

/**
 * 需求的一项变更意图，不改变系统当前态。只声明数据，不自行校验引用或创建实际对象。
 * targetType 决定显式路径终点：应用只用 projectApplicationId，模块止于 projectModuleId，
 * 功能止于 projectFunctionId；下级字段必须为空。
 * CREATE 的目标 ID 为空；已有祖先填实际 ID，新直接父对象由同一需求的 parentItemKey 引用。
 * UPDATE 使用完整已有路径，不支持移动父级。保存与归属校验由后续 Domain 实现承担。
 * 普通属性原样存取，无数据库、文件、模型调用或状态转换。
 */
public class RequirementChangePlanItem extends DataBean {

    /** 所属 Project.id 数据库主键；不是 Project.projectId 的字符串 UUID。 */
    private Long projectId;

    /** 所属 Requirement.id 数据库主键，必须属于同一项目。 */
    private Long requirementId;

    /** 同一需求内唯一的 UUID 规划键，由后续编排方提供；不是数据库外键，构造时不生成。 */
    private String itemKey;

    /** 预计修改的层级。 */
    private ProjectElementType targetType;

    /** 规划意图为 CREATE 或 UPDATE；不等同于后续每次实际修改的操作。 */
    private ProjectChangeOperation operation;

    /** 路径中的 ProjectApplication.id；新建应用时为空。 */
    private Long projectApplicationId;

    /** 路径中的 ProjectModule.id；目标为应用或模块尚未创建时为空。 */
    private Long projectModuleId;

    /** 目标 ProjectFunction.id；目标不是功能或功能尚未创建时为空。 */
    private Long projectFunctionId;

    /** 新建直接父对象的 CREATE 计划 itemKey；同一需求且相邻层级。已有父级及 UPDATE 时为空。 */
    private String parentItemKey;

    /** 预计创建或修改后的目标名称。 */
    private String targetName;

    /** 本项预计修改的内容与范围。 */
    private String changeDescription;

    /** 本项明确保持不变的行为，用于限制修改边界。 */
    private String unchangedBehavior;

    /** 本项变更的验收标准。 */
    private String acceptanceCriteria;

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

    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
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

    public String getParentItemKey() {
        return parentItemKey;
    }

    public void setParentItemKey(String parentItemKey) {
        this.parentItemKey = parentItemKey;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public String getUnchangedBehavior() {
        return unchangedBehavior;
    }

    public void setUnchangedBehavior(String unchangedBehavior) {
        this.unchangedBehavior = unchangedBehavior;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }
}
