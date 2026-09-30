/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存模块中一个具体功能的当前说明与实现引用。
 */
package com.kk24426.zbagentwf.common.project.model;

import java.util.List;

import com.kk24426.zbagentwf.common.DataBean;

/**
 * 模块下可独立描述的具体功能，例如新增订单或修改订单；描述当前实际行为，不复制完整代码或接口定义。
 * 普通属性原样存取，无数据库、文件、模型调用或状态转换。
 */
public class ProjectFunction extends DataBean {

    /** 所属 ProjectModule.id 数据库主键。 */
    private Long projectModuleId;

    /** 具体功能名称。 */
    private String name;

    /** 当前功能的简短摘要，便于检索和规划上下文选择。 */
    private String summary;

    /** 当前功能的详细行为、输入输出及业务约束说明。 */
    private String description;

    /** 可空的代码位置、页面路径或 API 引用；普通 Bean 原样存取，不读取引用内容。 */
    private List<String> implementationRefs;

    public Long getProjectModuleId() {
        return projectModuleId;
    }

    public void setProjectModuleId(Long projectModuleId) {
        this.projectModuleId = projectModuleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImplementationRefs() {
        return implementationRefs;
    }

    public void setImplementationRefs(List<String> implementationRefs) {
        this.implementationRefs = implementationRefs;
    }
}
