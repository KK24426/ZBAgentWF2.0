/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：以不可变强类型数据保存应用、模块和功能快照。
 */
package com.kk24426.zbagentwf.common.project.model;

import java.util.List;

/**
 * 变更提交候选及实际履历共用的强类型快照；三种记录不持有可变实体引用。
 * 候选新增对象可暂缺数据库生成的 id、直接父级 ID 和 version；已保存历史必须完整。
 * version 可空用于区分候选未生成值与持久化版本 0，不继承可变 DataBean。
 * 本类型不校验业务归属，不生成身份、不执行持久化；最终身份、版本和删除标识由后续发布实现负责。
 */
public sealed interface ProjectElementSnapshot
        permits ProjectElementSnapshot.ApplicationSnapshot,
                ProjectElementSnapshot.ModuleSnapshot,
                ProjectElementSnapshot.FunctionSnapshot {

    /**
     * 应用当前值的不可变副本；构造只保存值，无外部副作用。
     * @param id ProjectApplication.id；新增候选尚未保存时可空
     * @param projectId Project.id 数据库主键，不是字符串 UUID；保存历史时必填
     * @param name 应用名称
     * @param description 应用职责说明
     * @param version 持久化版本；新增候选尚未生成时可空
     * @param delFlg 删除标识快照，不是删除请求；本轮不允许通过候选改变此标识
     */
    record ApplicationSnapshot(Long id, Long projectId, String name, String description,
                               Integer version, boolean delFlg) implements ProjectElementSnapshot {
    }

    /**
     * 模块当前值的不可变副本；构造只保存值，无外部副作用。
     * @param id ProjectModule.id；新增候选尚未保存时可空
     * @param projectApplicationId 直接父级 ProjectApplication.id；候选新父级未解析时可空，历史必填
     * @param name 模块名称
     * @param description 模块职责说明
     * @param version 持久化版本；新增候选尚未生成时可空
     * @param delFlg 删除标识快照，不是删除请求；本轮不允许通过候选改变此标识
     */
    record ModuleSnapshot(Long id, Long projectApplicationId, String name, String description,
                          Integer version, boolean delFlg) implements ProjectElementSnapshot {
    }

    /**
     * 功能当前值的不可变副本；实现引用不会被读取或执行。
     * @param id ProjectFunction.id；新增候选尚未保存时可空
     * @param projectModuleId 直接父级 ProjectModule.id；候选新父级未解析时可空，历史必填
     * @param name 功能名称
     * @param summary 简短摘要
     * @param description 当前详细行为说明
     * @param implementationRefs 可空的实现引用；非空列表复制为不可修改的独立列表，元素不可为 null
     * @param version 持久化版本；新增候选尚未生成时可空
     * @param delFlg 删除标识快照，不是删除请求；本轮不允许通过候选改变此标识
     */
    record FunctionSnapshot(Long id, Long projectModuleId, String name, String summary,
                            String description, List<String> implementationRefs,
                            Integer version, boolean delFlg) implements ProjectElementSnapshot {

        /**
         * 防御复制可选引用列表，使后续调用方修改列表不影响历史；不进行业务校验或外部调用。
         * @throws NullPointerException 非空 implementationRefs 列表含 null 元素
         */
        public FunctionSnapshot {
            implementationRefs = implementationRefs == null ? null : List.copyOf(implementationRefs);
        }
    }
}
