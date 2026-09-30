/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存应用中一个模块的当前信息。
 */
package com.kk24426.zbagentwf.common.project.model;

import com.kk24426.zbagentwf.common.DataBean;

/**
 * 模块只属于一个应用；当前仅一层模块，不支持递归模块。同名跨应用模块彼此独立。
 * 普通属性原样存取，无数据库、文件、模型调用或状态转换。
 */
public class ProjectModule extends DataBean {

    /** 所属 ProjectApplication.id 数据库主键。 */
    private Long projectApplicationId;

    /** 模块名称。 */
    private String name;

    /** 当前模块的用途与职责说明。 */
    private String description;

    public Long getProjectApplicationId() {
        return projectApplicationId;
    }

    public void setProjectApplicationId(Long projectApplicationId) {
        this.projectApplicationId = projectApplicationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
