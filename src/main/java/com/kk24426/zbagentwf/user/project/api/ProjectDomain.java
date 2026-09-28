/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义项目创建、项目内需求规划与任务结果汇总的契约。
 */
package com.kk24426.zbagentwf.user.project.api;

import java.util.List;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Objects;

import com.kk24426.zbagentwf.common.DataBeanDao;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.project.model.AgentRole;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.common.project.model.Requirement;
import com.kk24426.zbagentwf.common.project.model.RequirementTask;
import com.kk24426.zbagentwf.user.UserInterface;

/** 项目包含多条需求，需求包含多个 Task；具体实现位于 agent.project。 */
public abstract class ProjectDomain implements UserInterface {

	protected final DataBeanDao<Project> dao;

	/** 注入完整项目聚合的持久化实现；构造不访问数据库。 */
	protected ProjectDomain(DataBeanDao<Project> dao) { this.dao = Objects.requireNonNull(dao); }

	/**
	 * 显式三角色创建项目；先建立UUID及角色上下文，再规划首批需求并保存完整聚合。
	 * 具体实现负责root/local/UUID目录校验；模型调用与数据库提交分离，失败不得假装创建成功。
	 * @param content 非空项目描述
	 * @param projectName 可空项目名称
	 * @param planAgent 规划模型
	 * @param developmentAgent 开发模型
	 * @param reviewAgent 保留的业务审核角色；提示词安全审核使用各调用自身模型
	 * @return 已保存、具有数据库主键和业务UUID的项目
	 */
	public Project newProject(String content, String projectName, AgentBean planAgent, AgentBean developmentAgent,
			AgentBean reviewAgent) {
		Project project = new Project(projectName, new ArrayList<>(), planAgent, developmentAgent, reviewAgent);
		project.setProjectId(UUID.randomUUID().toString());
		project.setRequirements(createRequiremensOnNew(project, content));
		dao.insert(project);
		return project;
	}

	/** 使用显式配置的三角色默认模型；默认缺失时失败，不推断替代模型。 */
	public Project newProject(String content, String projectName) {
		return newProject(content, projectName, getAgent(AgentRole.PLANNING), getAgent(AgentRole.DEVELOPMENT),
				getAgent(AgentRole.REVIEW));

	}

	/** 按项目UUID追加非空规则文本并保存；不在追加时调用模型，下一次完整调用仍需安全审核。 */
	public abstract void addProjectPrompt(String projectId, String Prompt);

	/** 取得指定角色的默认模型快照并绑定角色规则，缺配置抛未就绪异常。 */
	protected abstract AgentBean getAgent(AgentRole agentTypeEnum);

	/** 新建时先规划需求，再为信息充分的需求拆任务；返回未发布列表，失败不能部分加入项目。 */
	protected abstract List<Requirement> createRequiremensOnNew(Project project, String content);

	/** 对已保存项目完整规划并追加本次需求，保存成功后返回新增列表；不得覆盖原需求或任务。 */
	public abstract List<Requirement> createRequiremens(Project p, String content);

	/** 只为本项目内尚无任务且无需确认的需求拆任务并保存；已有任务明确拒绝，不覆盖。 */
	public abstract List<RequirementTask> createRequirementsTask(Project p, Requirement requirement);

	/**
	 * 等待异步批次结束后返回含Task终态的需求列表；失败、待确认或既有RUNNING停止后续执行。
	 * 调用线程不得持有project锁等待worker；数据库保存不确定时抛异常，重载后也不自动重跑。
	 */
	public abstract List<Requirement> execTask(Project project);

	/** 按业务UUID完整重载数据库快照并保留本进程项目身份；未知/已删除项目失败，不从目录重建。 */
	public abstract Project getProject(String projectId);
}
