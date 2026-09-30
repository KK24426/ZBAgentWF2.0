/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存项目中一个已存在应用端的当前信息。
 */
package com.kk24426.zbagentwf.common.project.model;

import com.kk24426.zbagentwf.common.DataBean;

/**
 * 项目下的独立应用端，例如后台、前台、App 或 API 服务；不表示待实施计划。
 * 普通属性原样存取，无数据库、文件、模型调用或状态转换。
 */
public class ProjectApplication extends DataBean {

    /** 所属 Project.id 数据库主键；不是 Project.projectId 的字符串 UUID。 */
    private Long projectId;

    /** 应用名称。 */
    private String name;

    /** 当前应用的用途与职责说明。 */
    private String description;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
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
