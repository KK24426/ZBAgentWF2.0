/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：声明项目结构中可规划和记录修改的对象层级。
 */
package com.kk24426.zbagentwf.common.project.model;

/** 对应应用、单层模块和具体功能，不包含页面或按钮的独立结构层级。 */
public enum ProjectElementType {
    /** 项目中的应用端。 */
    APPLICATION,
    /** 一个应用内的模块。 */
    MODULE,
    /** 一个模块内的具体功能。 */
    FUNCTION
}
