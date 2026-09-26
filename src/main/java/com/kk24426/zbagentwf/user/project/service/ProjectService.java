package com.kk24426.zbagentwf.user.project.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.kk24426.zbagentwf.common.project.bean.Project;
import com.kk24426.zbagentwf.user.project.domain.ProjectDomain;

@Service
public class ProjectService {

	private final ProjectDomain domian;

	public ProjectService(ProjectDomain domian) {
		this.domian = domian;
	}

	public Project newProject(String content) {
		if (!StringUtils.hasLength(content)) {
			// 这种需要返回给用户看的错误信息,帮我修改成调用msg
			throw new IllegalArgumentException("项目内容不能为空");
		}
		return domian.newProject(content);
	}

	public void addProjectPrompt(Project p, String content) {
		if (!StringUtils.hasLength(content)) {
			// 这种需要返回给用户看的错误信息,帮我修改成调用msg
			throw new IllegalArgumentException("内容不能为空");
		}
		domian.addProjectPrompt(p.getProjectId(), content);
	}

	public void createRequirements(Project p, String content) {
		if (!StringUtils.hasLength(content)) {
			// 这种需要返回给用户看的错误信息,帮我修改成调用msg
			throw new IllegalArgumentException("内容不能为空");
		}
		domian.createRequirements(p, content);
	}

	public void execTask(String projectId) {
		domian.execTask(domian.getProject(projectId));
	}

}
