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
public abstract class AgentExecutor implements UserInterface {
	private final AgentBean agent;

	/** 用户配置的提示词 */
	private Prompt userPrompt;

	/** 项目中使用的提示词 */
	private Prompt projectPrompt;

	public AgentExecutor(AgentBean agent) {
		super();
		this.agent = agent;
		this.userPrompt = null;
		this.projectPrompt = null;
	}

	/**
	 * 
	 * @param agent
	 * @param userPrompt    用户提示词
	 * @param projectPrompt 项目提示词
	 */
	public AgentExecutor(AgentBean agent, Prompt userPrompt, Prompt projectPrompt) {
		super();
		this.agent = agent;
		this.userPrompt = userPrompt;
		this.projectPrompt = projectPrompt;
	}

	protected Prompt getUserPrompt() {
		return userPrompt;
	}

	private void setUserPrompt(Prompt userPrompt) {
		this.userPrompt = userPrompt;
	}

	protected Prompt getProjectPrompt() {
		return projectPrompt;
	}

	private void setProjectPrompt(Prompt projectPrompt) {
		this.projectPrompt = projectPrompt;
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
	protected AgentBean getAgent() {
		return agent;
	}

	/**
	 * 异步受理一次执行；工作目录从 project 获取，不依赖全局“当前项目”。 受理后的成功、失败或需要确认均通过一次最终回调表达。
	 * 需要确认时本次执行结束，调用方收到答复后重新提交并获取新的执行标识。
	 *
	 * @param project  本次执行所属项目及工作目录
	 * @param content  用户指令
	 * @param memory   先前记忆，没有时可为空
	 * @param callback 最终结果回调；结果 taskId 与本方法返回值一致
	 * @return 本次执行的唯一标识，不是规划任务 id 或进程退出码
	 * @throws RejectedExecutionException 提交失败；未受理且不触发完成回调
	 */
	public abstract String exec(Project project, String content, String memory, AgentExecutionCallback callback);

	/**
	 * 按执行标识读取诊断文本；stderr 内容本身不决定成功或失败。
	 *
	 * @param taskId exec 返回的单次执行标识
	 * @return 对应执行的诊断文本，不得未经处理写入日志或公开错误响应 未知、其他执行器所有或已过期时返回
	 *         null；完成记录全应用最多256条、保留30分钟
	 */
	public abstract String getStderr(String taskId);

}
