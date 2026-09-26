/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义项目创建、项目内需求规划与任务结果汇总的契约。
 */
package com.kk24426.zbagentwf.user.project.userif;

import com.kk24426.zbagentwf.common.project.bean.AgentTypeEnum;
import com.kk24426.zbagentwf.common.project.bean.Project;
import com.kk24426.zbagentwf.common.project.bean.Requirement;
import com.kk24426.zbagentwf.user.UserInterface;
import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import java.util.List;

/** 项目包含多条需求，需求包含多个 Task；具体实现位于 agent.project。 */
public abstract class ProjectUserif implements UserInterface {
	/**
	 * 根据用户输入建立 UUID 项目目录，并生成首批需求。
	 *
	 * @param content          用户对项目的描述
	 * @param planningAgent    规划模型，创建时即用于生成首批需求
	 * @param developmentAgent 开发任务模型
	 * @param reviewAgent      审核模型，当前仅绑定，不自动启动审核
	 * @return 包含项目标识、工作目录和需求列表的项目对象
	 */
	public abstract Project newProject(String content, AgentBean planningAgent, AgentBean developmentAgent,
			AgentBean reviewAgent);

	/*
	 * public Project newProject(String content) {
	 * 
	 * return newProject(); }
	 * 
	 * public AgentBean getDefualtAgent(AgentTypeEnum agentTypeEnum){
	 * 
	 * }
	 */
	
	/**
	 * 
	 * @return
	 */
	/*
	 * private AgentBean getPlanAgent() {
	 * 
	 * 
	 * }
	 */
	

	/**
	 * 将用户需求规划为项目内的需求列表，每条需求包含可执行 Task。
	 *
	 * @param project 需求归属的项目
	 * @param content 用户需求
	 * @return 本次新增的需求及 Task 列表；生成成功后追加到项目
	 */
	public abstract List<Requirement> createRequirements(Project project, String content);

	/**
	 * 等待底层异步执行结束后汇总结果；需要确认同样代表该次底层执行已结束。
	 *
	 * @param project      本次执行所属项目及工作目录
	 * @param requirements 从该项目中选定的本次需求范围，其中 Task 均属于对应需求
	 * @return 带有 Task 状态和执行结果的需求列表，不是异步批次标识
	 */
	public abstract List<Requirement> execTask(Project project, List<Requirement> requirements);
}
