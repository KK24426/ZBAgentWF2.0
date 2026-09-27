/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义项目创建、项目内需求规划与任务结果汇总的契约。
 */
package com.kk24426.zbagentwf.user.project.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.kk24426.zbagentwf.common.DataBeanDao;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.project.model.AgentRole;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.common.project.model.Requirement;
import com.kk24426.zbagentwf.common.project.model.RequirementTask;
import com.kk24426.zbagentwf.user.UserInterface;

/** 项目包含多条需求，需求包含多个 Task；具体实现位于 agent.project。 */
public abstract class ProjectDomain implements UserInterface {

	@Autowired
	DataBeanDao<Project> dao;

	/**
	 * 根据用户输入建立 UUID 项目目录，并生成首批需求。
	 *
	 * @param content          用户对项目的描述
	 * @param planningAgent    规划模型，创建时即用于生成首批需求
	 * @param developmentAgent 开发任务模型
	 * @param reviewAgent      审核模型，当前仅绑定，不自动启动审核
	 * @return 包含项目标识、工作目录和需求列表的项目对象
	 */
	public Project newProject(String content, String projectName, AgentBean planAgent, AgentBean developmentAgent,
			AgentBean reviewAgent) {
		List<Requirement> requirements = createRequiremensOnNew(content);
		Project project = new Project(projectName, requirements, reviewAgent, reviewAgent, reviewAgent);
		dao.insert(project);
		return project;
	}

	public Project newProject(String content, String projectName) {
		return newProject(content, projectName, getAgent(AgentRole.PLANNING), getAgent(AgentRole.DEVELOPMENT),
				getAgent(AgentRole.REVIEW));

	}

	/**
	 * 添加项目提示词
	 * 
	 * @param projectId
	 * @param Prompt
	 */
	public abstract void addProjectPrompt(String projectId, String Prompt);

	/**
	 * 根据不同的类型生成对应的Agent实体并注入角色提示词
	 * 
	 * @param agentTypeEnum Agent类型
	 * @return
	 */
	protected abstract AgentBean getAgent(AgentRole agentTypeEnum);

	/**
	 * 新建项目时将用户的原始需求规划为项目内的需求列表
	 *
	 * @param project 需求归属的项目
	 * @param content 用户需求
	 * @return 本次新增的需求及 Task 列表；生成成功后追加到项目
	 */
	protected abstract List<Requirement> createRequiremensOnNew(String content);

	/**
	 * 将用户的原始需求规划为项目内的需求列表
	 *
	 * @param project 需求归属的项目
	 * @param content 用户需求
	 * @return 本次新增的需求及 Task 列表；生成成功后追加到项目
	 */
	public abstract List<Requirement> createRequiremens(Project p, String content);

	/**
	 * 将项目内的需求规划为具体的可执行的Task列表
	 *
	 * @param project 需求归属的项目
	 * @param content 用户需求
	 * @return 本次新增的需求及 Task 列表；生成成功后追加到项目
	 */
	public abstract List<RequirementTask> createRequirementsTask(Project p, Requirement requirement);

	/**
	 * 等待底层异步执行结束后汇总结果；需要确认同样代表该次底层执行已结束。
	 *
	 * @param project 本次执行所属项目及工作目录
	 * @return 带有 Task 状态和执行结果的需求列表，不是异步批次标识
	 */
	public abstract List<Requirement> execTask(Project project);

	public abstract Project getProject(String projectId);
}
