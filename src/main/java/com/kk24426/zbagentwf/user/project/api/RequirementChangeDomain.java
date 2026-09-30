/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义需求变更计划及任务修改边界的抽象契约。
 */
package com.kk24426.zbagentwf.user.project.api;

import java.util.List;

import com.kk24426.zbagentwf.common.project.model.RequirementChangePlanItem;
import com.kk24426.zbagentwf.common.project.model.TaskChangePlanLink;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 维护需求预计修改的内容与任务负责范围；本轮仅声明契约，没有持久化或任务编排实现。
 * 后续实现须校验项目、需求、任务及计划项的完整归属，拒绝不匹配的数据，
 * 不允许以保存计划或任务关联提前修改系统当前态，也不自动执行任务或改变任务状态。
 * projectId 为 Project.id 数据库主键；其他外键同样采用完整实体类名加 Id，不使用业务 UUID。
 * 查询返回独立数据对象并按 id 升序；无结果或指定对象不属于项目时返回空列表。
 */
public abstract class RequirementChangeDomain implements UserInterface {

    /**
     * 为已有需求整体新增一批计划，成功后返回已保存的新增项；不覆盖已有计划。
     * 计划自身 id 应为空，itemKey 由调用方提供且在同一需求中唯一，不由 Bean 自动生成。
     * CREATE 的目标 ID 必须为空，已有祖先用实际 ID；新直接父级使用同一需求、相邻层级
     * CREATE 计划的 parentItemKey，可引用本批或同一需求中已保存的计划。
     * APPLICATION 路径不含模块/功能，MODULE 路径不含功能，FUNCTION 使用完整祖先路径。
     * UPDATE 必须引用已有对象与其完整归属路径，parentItemKey 为空，不允许移动父级。
     * 必须拒绝重复规划键、错误层级、无效父引用、跨项目/需求或矛盾祖先路径；
     * 不能静默覆盖冲突 ID。任何一项失败不发布本批局部计划，不创建实际对象。
     * @param projectId 所属 Project.id 数据库主键
     * @param requirementId 已存在且属于项目的 Requirement.id 数据库主键
     * @param plans 待整体新增的计划数据，其 projectId/requirementId 必须与参数一致
     * @return 成功保存后的新增计划独立数据列表，按 id 升序
     */
    public abstract List<RequirementChangePlanItem> createPlans(
            Long projectId, Long requirementId, List<RequirementChangePlanItem> plans);

    /**
     * 查询指定需求的变更计划，不把计划解释成已存在的功能。
     * @param projectId 所属 Project.id 数据库主键
     * @param requirementId 目标 Requirement.id 数据库主键
     * @return 该需求计划的独立数据列表，按 id 升序，无结果时为空列表
     */
    public abstract List<RequirementChangePlanItem> getPlans(Long projectId, Long requirementId);

    /**
     * 为一个已存在的 PENDING 任务整体设置计划关联，成功后返回新的完整关联集合。
     * 所有计划项必须属于该需求，任务也必须属于同一需求；校验或保存失败不留下局部替换。
     * 一个任务只能负责同一模块自身及其多个功能，不能跨模块或跨应用；
     * 新模块须用 CREATE 计划 itemKey 确定身份，不能把多个 null 主键视为同一模块。
     * 应用级变更使用独立任务，只关联同一应用的应用级计划，不与模块/功能变更混合。
     * 本操作只设置边界，不推进任务状态、不执行变更、不创建当前态。
     * @param projectId 所属 Project.id 数据库主键
     * @param requirementId 所属 Requirement.id 数据库主键
     * @param requirementTaskId 已存在且仍为 PENDING 的 RequirementTask.id 数据库主键
     * @param requirementChangePlanItemIds 任务负责的完整计划主键集合
     * @return 已保存的完整关联独立数据列表，按 id 升序
     */
    public abstract List<TaskChangePlanLink> setTaskPlans(
            Long projectId, Long requirementId, Long requirementTaskId,
            List<Long> requirementChangePlanItemIds);

    /**
     * 查询任务关联的计划项，以便规划或执行方读取明确的修改边界。
     * @param projectId 所属 Project.id 数据库主键
     * @param requirementTaskId 目标 RequirementTask.id 数据库主键
     * @return 任务关联计划的独立数据列表，按计划 id 升序，无结果时为空列表
     */
    public abstract List<RequirementChangePlanItem> getTaskPlans(Long projectId, Long requirementTaskId);
}
