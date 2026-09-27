/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存项目标识、工作目录、三角色执行器及其需求列表。
 */
package com.kk24426.zbagentwf.common.project.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.kk24426.zbagentwf.common.DataBean;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.agent.model.Prompt;

/** 保存项目标识、工作目录、三角色执行器及其需求列表。 普通属性原样存取，不自动执行业务校验。 */
public class Project extends DataBean {
	/** 项目标识，不等同于一次 Agent 执行标识。 */
	private String projectId;

	/** 项目名 */
	private String projectName;

	/** 属于本项目的需求；默认列表由每个项目独立持有。 */
	private List<Requirement> requirements = new ArrayList<>();

	/** 规划模型信息 */
	private AgentBean planningAgent;
	/** 开发 模型 */
	private AgentBean developmentAgent;
	/** 审核模型 */
	private AgentBean reviewAgent;
	
	public Project(String projectName, List<Requirement> requirements, AgentBean planningAgent,
			AgentBean developmentAgent, AgentBean reviewAgent) {
		super();
		this.projectName = projectName;
		this.requirements = requirements;
		this.planningAgent = planningAgent;
		this.developmentAgent = developmentAgent;
		this.reviewAgent = reviewAgent;
	}

	/** 项目中使用的提示词 */
	private Prompt projectPrompt;

	private String getProjectName() {
		return projectName;
	}

	private void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Prompt getProjectPrompt() {
		return projectPrompt;
	}

	public void setProjectPrompt(Prompt projectPrompt) {
		this.projectPrompt = projectPrompt;
	}

	public AgentBean getPlanningAgent() {
		return planningAgent;
	}

	public void setPlanningAgent(AgentBean planningAgent) {
		this.planningAgent = planningAgent;
	}

	public AgentBean getDevelopmentAgent() {
		return developmentAgent;
	}

	public void setDevelopmentAgent(AgentBean developmentAgent) {
		this.developmentAgent = developmentAgent;
	}

	public AgentBean getReviewAgent() {
		return reviewAgent;
	}

	public void setReviewAgent(AgentBean reviewAgent) {
		this.reviewAgent = reviewAgent;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public List<Requirement> getRequirements() {
		return requirements;
	}

	public void setRequirements(List<Requirement> requirements) {
		this.requirements = requirements;
	}
}
