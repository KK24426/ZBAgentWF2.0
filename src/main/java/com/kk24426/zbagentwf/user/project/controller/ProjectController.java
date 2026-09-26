/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：接收单次聊天请求、调用用户服务并返回安全的结果或固定错误。
 */
package com.kk24426.zbagentwf.user.project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import com.kk24426.zbagentwf.user.project.service.ProjectService;

/** 项目画面用 */
@RestController
public class ProjectController {
	private static final Logger LOG = LoggerFactory.getLogger(ProjectController.class);
	private final ProjectService projectService;
	private final MsgCatalog messages;

	public ProjectController(ProjectService projectService, MsgCatalog messages) {
		this.projectService = projectService;
		this.messages = messages;
	}
}
