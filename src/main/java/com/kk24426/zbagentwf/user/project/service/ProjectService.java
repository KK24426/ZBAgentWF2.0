/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：校验项目输入并通过用户定义的领域接口编排操作。
 */
package com.kk24426.zbagentwf.user.project.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.kk24426.zbagentwf.common.agent.model.AgentBean;

import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.user.project.api.ProjectDomain;

/** 保持用户的编排入口，校验文本并委托领域实现；不在服务层持锁等待异步任务。 */
@Service
public class ProjectService {

	private final ProjectDomain domain;

	/** 绑定领域服务，构造不查询数据库或启动模型。 */
	public ProjectService(ProjectDomain domain) {
		this.domain = domain;
	}

	/** 使用显式默认角色创建可选名称项目；空白描述在目录或模型副作用前拒绝。 */
	public Project newProject(String content, String projectName) {
		if (!StringUtils.hasText(content)) {
			// 内部校验异常不直接返回给用户，Controller 通过 MsgCatalog 渲染固定提示。
			throw new IllegalArgumentException("项目内容不能为空");
		}
		return domain.newProject(content, projectName);
	}

	/** 使用调用方完整选择的三个角色创建项目，不按缺失角色回退默认。 */
	public Project newProject(String content, String projectName, AgentBean planning, AgentBean development,
			AgentBean review) {
		if (!StringUtils.hasText(content))
			throw new IllegalArgumentException("项目内容不能为空。");
		return domain.newProject(content,projectName, planning, development, review);
	}

	public Project getProject(String projectId) {
		return domain.getProject(projectId);
	}

	/** 追加非空规则并保存；规则在后续完整调用中再次接受审核。 */
	public void addProjectPrompt(Project p, String content) {
		if (!StringUtils.hasText(content)) {
			// 内部校验异常不直接返回给用户，Controller 通过 MsgCatalog 渲染固定提示。
			throw new IllegalArgumentException("内容不能为空");
		}
		domain.addProjectPrompt(p.getProjectId(), content);
	}

	/** 追加需求先完整两阶段规划，再交领域保存；不覆盖已有任务。 */
	public void createRequirements(Project p, String content) {
		if (!StringUtils.hasText(content)) {
			// 内部校验异常不直接返回给用户，Controller 通过 MsgCatalog 渲染固定提示。
			throw new IllegalArgumentException("内容不能为空");
		}
		domain.createRequiremens(p,content);
	}

	/** 按UUID重载并等待领域批次结束；本线程不占项目锁，保存异常继续抛出。 */
	public void execTask(String projectId) {
		domain.execTask(domain.getProject(projectId));
	}

}
