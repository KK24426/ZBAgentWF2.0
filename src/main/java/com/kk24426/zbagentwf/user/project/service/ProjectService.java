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

@Service
public class ProjectService {

	private final ProjectDomain domain;

	public ProjectService(ProjectDomain domain) {
		this.domain = domain;
	}

	public Project newProject(String content) {
		if (!StringUtils.hasText(content)) {
			// 内部校验异常不直接返回给用户，Controller 通过 MsgCatalog 渲染固定提示。
			throw new IllegalArgumentException("项目内容不能为空");
		}
		return domain.newProject(content);
	}

    public Project newProject(String content, AgentBean planning, AgentBean development, AgentBean review) {
        if (!StringUtils.hasText(content)) throw new IllegalArgumentException("项目内容不能为空。");
        return domain.newProject(content, planning, development, review);
    }

    public Project getProject(String projectId) { return domain.getProject(projectId); }

	public void addProjectPrompt(Project p, String content) {
		if (!StringUtils.hasText(content)) {
			// 内部校验异常不直接返回给用户，Controller 通过 MsgCatalog 渲染固定提示。
			throw new IllegalArgumentException("内容不能为空");
		}
		domain.addProjectPrompt(p.getProjectId(), content);
	}

	public void createRequirements(Project p, String content) {
		if (!StringUtils.hasText(content)) {
			// 内部校验异常不直接返回给用户，Controller 通过 MsgCatalog 渲染固定提示。
			throw new IllegalArgumentException("内容不能为空");
		}
		domain.createRequirements(p, content);
	}

	public void execTask(String projectId) {
		domain.execTask(domain.getProject(projectId));
	}

}
