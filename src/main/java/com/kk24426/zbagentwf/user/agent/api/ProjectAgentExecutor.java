/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义项目内单次异步 Agent 执行及按执行标识读取诊断的契约。
 */
package com.kk24426.zbagentwf.user.agent.api;

import java.util.concurrent.RejectedExecutionException;

import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.agent.model.Prompt;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.user.UserInterface;

/** 用户定义的执行契约；通过 AgentExecutorFactory 按已注册模型获取实现。 */
public abstract class ProjectAgentExecutor implements UserInterface {

	private final Project project;

	/** 用户配置的提示词 */
	private Prompt userPrompt;

	public ProjectAgentExecutor(Project project) {
		super();
		this.project = project;
		this.userPrompt = null;
	}

	/**
	 * 
	 * @param agent
	 * @param userPrompt    用户提示词
	 * @param projectPrompt 项目提示词
	 */
	public ProjectAgentExecutor(Project project, Prompt userPrompt) {
		super();
		this.project = project;
		this.userPrompt = userPrompt;
	}

	protected Prompt getUserPrompt() {
		return userPrompt;
	}

	/**
	 * 读取显式初始化后的默认提示词；不得在 getter 中加载文件
	 */
	protected abstract Prompt getDefaultPrompt();

	/**
	 * 读取显式初始化后的安全提示词；不得在 getter 中加载文件
	 */
	protected abstract Prompt getSecurityPrompt();

	/** 供实现类读取构造时传入的模型信息，不触发模型发现或执行。 */
	protected Project getProject() {
		return project;
	}

	/**
	 * 实际执行应该调用这个方法
	 */
	protected void exec() {

	}

	/**
	 * 执行Project中所有待执行的Task
	 * 
	 * @param callback
	 * @return
	 */
	public abstract String execAllTasks(AgentExecutionCallback callback);

	/**
	 * 执行Project中所有待执行的Task
	 * 
	 * @param callback
	 * @return
	 */
	public abstract String execTasks(AgentExecutionCallback callback);

	/**
	 * 按执行标识读取诊断文本；stderr 内容本身不决定成功或失败。
	 *
	 * @param taskId exec 返回的单次执行标识
	 * @return 对应执行的诊断文本，不得未经处理写入日志或公开错误响应 未知、其他执行器所有或已过期时返回
	 *         null；完成记录全应用最多256条、保留30分钟
	 */
	public abstract String getStderr(String taskId);

	/**
	 * Agent执行之前必须调用本函数进行安全性检查,<br/>
	 * 使用默认的模型分析提示词,检查有无安全风险,注入攻击,无意义内容
	 * 
	 * @param prompt
	 * @return
	 */
	protected abstract boolean securityCheck();

	/**
	 * 获取所有的提示词内容
	 * 
	 * @return
	 */
	public abstract Prompt getAllPrompt();

}
