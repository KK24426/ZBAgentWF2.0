/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存项目标识、工作目录及其需求列表。
 */
package com.kk24426.zbagentwf.common.project.bean;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 保存项目标识、工作目录及其需求列表。 普通属性原样存取，不自动执行业务校验。 */
public class Project {
    /** 项目标识，不等同于一次 Agent 执行标识。 */
    private String projectId;

    /** 项目自身的工作目录；与全局项目根目录区分。 */
    private Path workingDirectory;

    /** 属于本项目的需求；默认列表由每个项目独立持有。 */
    private List<Requirement> requirements = new ArrayList<>();

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<Requirement> requirements) {
        this.requirements = requirements;
    }
}
