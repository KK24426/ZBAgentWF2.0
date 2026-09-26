/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存项目标识、工作目录、三角色执行器及其需求列表。
 */
package com.kk24426.zbagentwf.common.project.bean;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecutor;

/** 保存项目标识、工作目录、三角色执行器及其需求列表。 普通属性原样存取，不自动执行业务校验。 */
public class Project {
    /** 项目标识，不等同于一次 Agent 执行标识。 */
    private String projectId;

    /** 项目自身的工作目录；与全局项目根目录区分。 */
    private Path workingDirectory;

    /** 属于本项目的需求；默认列表由每个项目独立持有。 */
    private List<Requirement> requirements = new ArrayList<>();

    /** 项目规划执行器；运行时引用，由工厂管理生命周期。 */
    private AgentExecutor planningAgent;
    /** 开发 Task 使用的执行器；不在项目之间共享需求上下文。 */
    private AgentExecutor developmentAgent;
    /** 审核执行器；当前仅绑定，尚无自动审核业务流程。 */
    private AgentExecutor reviewAgent;

    public AgentExecutor getPlanningAgent() { return planningAgent; }
    public void setPlanningAgent(AgentExecutor planningAgent) { this.planningAgent = planningAgent; }
    public AgentExecutor getDevelopmentAgent() { return developmentAgent; }
    public void setDevelopmentAgent(AgentExecutor developmentAgent) { this.developmentAgent = developmentAgent; }
    public AgentExecutor getReviewAgent() { return reviewAgent; }
    public void setReviewAgent(AgentExecutor reviewAgent) { this.reviewAgent = reviewAgent; }

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
