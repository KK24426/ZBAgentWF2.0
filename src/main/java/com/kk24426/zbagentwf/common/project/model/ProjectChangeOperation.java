/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：声明规划意图和实际修改支持的操作。
 */
package com.kk24426.zbagentwf.common.project.model;

/** 本轮仅支持创建与修改，不定义删除、停用、迁移或任何状态转换。 */
public enum ProjectChangeOperation {
    /** 创建新对象。 */
    CREATE,
    /** 修改已存在对象，归属父级保持不变。 */
    UPDATE
}
