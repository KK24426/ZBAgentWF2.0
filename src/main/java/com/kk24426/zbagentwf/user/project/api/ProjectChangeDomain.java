/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义已确认实际修改的发布和履历查询抽象契约。
 */
package com.kk24426.zbagentwf.user.project.api;

import java.util.List;

import com.kk24426.zbagentwf.common.project.model.ProjectChangeRecord;
import com.kk24426.zbagentwf.common.project.model.ProjectChangeSubmission;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 发布已核实的实际变更并查询履历；本轮只声明未来实现约束，无数据库或代码执行能力。
 * 不充当代码执行器或验收判定器，不依据任务成功状态推断实际修改，也不自动重试或转换任务状态。
 * 历史只追加，无修改/删除入口；返回数据与内部可变对象分离，列表按履历 id 升序。
 * 所有查询均受 projectId 范围限制；无结果或目标归属不匹配时返回空列表。
 * projectId 为 Project.id 数据库主键，不是 Project.projectId 字符串 UUID；
 * requirementTaskId 和 requirementChangePlanItemId 分别指完整命名实体的数据库主键。
 */
public abstract class ProjectChangeDomain implements UserInterface {

    /**
     * 将一个任务中已核实的实际修改整体发布为当前态与不可改写的履历。
     * 以下均是后续实现要求，本抽象类不提供事务、版本校验或幂等实现：
     * <ul>
     *   <li>校验项目、需求、任务、已分配计划项、对象层级和归属路径一致，
     *       不得超出计划内容、保持不变行为及任务单模块边界；批内计划项不能重复。</li>
     *   <li>实际 CREATE 的 beforeSnapshot 为空；实际 UPDATE 的 beforeSnapshot 为完整持久化快照，
     *       其 id、父级和类型匹配实际对象，version 必须与当前版本一致；冲突拒绝发布。
     *       afterSnapshot 必须非空且类型匹配，不允许移动父级或通过 delFlg 隐含删除。</li>
     *   <li>CREATE 计划第一次成功发布时由 CREATE 履历绑定唯一实际对象；
     *       后续任务沿该计划修改时操作为 UPDATE，必须提供该对象的修改前快照，不能再次创建。
     *       UPDATE 计划只能修改其既有目标。子对象发布前父对象必须已创建并解析出实际主键。</li>
     *   <li>允许新增候选暂缺生成主键、直接父级主键与版本，但不能把候选元数据当作保存结果。
     *       parentItemKey 只能解析到同一需求的正确父层 CREATE 计划实际对象，
     *       调用方已给出的祖先 ID 不得与解析结果冲突。
     *       最终履历的路径、前后快照身份、版本及删除标识由实际保存结果确定。</li>
     *   <li>当前态更新和本批履历追加须在同一数据库事务中完成，失败不能发布局部结果；
     *       此原子性不包括已发生的代码或文件变更，保存不确定时不得自动重跑任务。</li>
     *   <li>以 (projectId, requirementTaskId, executionId, requirementChangePlanItemId)
     *       识别单项重复提交。相同请求返回原记录，不重复更新或生成履历；
     *       同一键的不同请求必须拒绝，重放应先于会因首次发布而变化的版本/CREATE 状态校验。</li>
     * </ul>
     * 失败任务中已核实的局部实际修改也可提交；这里只发布提交的整体集合，不表示整个任务验收成功。
     * @param projectId 所属 Project.id 数据库主键
     * @param requirementId 所属 Requirement.id 数据库主键
     * @param requirementTaskId 执行本次修改的 RequirementTask.id 数据库主键
     * @param executionId 本次任务单次执行的 UUID，不是批次 ID 或数据库外键
     * @param changes 已确认发生、关联本任务计划的修改候选；每个计划项至多出现一次
     * @return 成功保存或幂等重放得到的独立履历数据列表，按履历 id 升序
     */
    public abstract List<ProjectChangeRecord> publishTaskChanges(
            Long projectId, Long requirementId, Long requirementTaskId,
            String executionId, List<ProjectChangeSubmission> changes);

    /**
     * 查询应用对象自身的修改，不自动汇总其模块和功能的履历。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectApplicationId 目标 ProjectApplication.id 数据库主键
     * @return targetType 为 APPLICATION 且目标匹配的履历，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectChangeRecord> getChangesByApplication(Long projectId, Long projectApplicationId);

    /**
     * 查询模块对象自身的修改，不包含其功能的履历。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectModuleId 目标 ProjectModule.id 数据库主键
     * @return targetType 为 MODULE 且目标匹配的履历，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectChangeRecord> getChangesByModule(Long projectId, Long projectModuleId);

    /**
     * 查询一个具体功能的修改履历。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectFunctionId 目标 ProjectFunction.id 数据库主键
     * @return targetType 为 FUNCTION 且目标匹配的履历，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectChangeRecord> getChangesByFunction(Long projectId, Long projectFunctionId);

    /**
     * 查询一个需求已经产生的全部实际修改，包含该需求下各任务及各目标层级。
     * @param projectId 所属 Project.id 数据库主键
     * @param requirementId 目标 Requirement.id 数据库主键
     * @return 该需求的履历，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectChangeRecord> getChangesByRequirement(Long projectId, Long requirementId);

    /**
     * 查询一个任务已经产生的实际修改，包括不同 executionId 的独立记录。
     * @param projectId 所属 Project.id 数据库主键
     * @param requirementTaskId 目标 RequirementTask.id 数据库主键
     * @return 该任务的履历，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectChangeRecord> getChangesByTask(Long projectId, Long requirementTaskId);
}
