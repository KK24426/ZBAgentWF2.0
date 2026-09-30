/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存任务待发布的单项实际修改候选。
 */
package com.kk24426.zbagentwf.common.project.model;

/**
 * 一项实际修改的不可变提交数据；构造只保存值，不确认验收、改变任务状态或写入当前态。
 * 后续发布实现通过计划项找到对象；候选不具有自行决定持久化身份、版本或删除标识的权限。
 * @param requirementChangePlanItemId 已分配给当前任务的 RequirementChangePlanItem.id 数据库主键
 * @param beforeSnapshot 修改前的完整持久化快照；实际 CREATE 时为空，实际 UPDATE 时必填
 * @param afterSnapshot 修改后的候选快照，必须非空且与目标层级一致；允许缺失尚未生成的元数据
 * @param changeDescription 已核实实际发生的修改说明，不是执行代码的指令
 */
public record ProjectChangeSubmission(Long requirementChangePlanItemId,
                                      ProjectElementSnapshot beforeSnapshot,
                                      ProjectElementSnapshot afterSnapshot,
                                      String changeDescription) {
}
