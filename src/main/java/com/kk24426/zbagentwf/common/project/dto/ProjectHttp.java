/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义项目 HTTP 输入及不含运行时引用的响应快照。
 */
package com.kk24426.zbagentwf.common.project.dto;

import com.kk24426.zbagentwf.common.agent.model.AgentBean;
import com.kk24426.zbagentwf.common.agent.model.AgentExecutionResult;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.common.project.model.TaskStatus;
import java.util.List;

/** HTTP 只接受模型标识和文本，不能接收客户端提供的目录、执行器或 Project 实体。 */
public final class ProjectHttp {
	private ProjectHttp() {
	}

	/**
	 * 客户端模型选择，仅含精确三元组；可用性和角色规则由服务器另行校验。
	 */
	public record Model(String brand, String name, String ver) {
		/** 仅检查三元组完整且非空白；不裁剪、推断版本或查询可用性。 */
		public boolean valid() {
			return text(brand) && text(name) && text(ver);
		}

		/**
		 * 将三元组原样装入新Bean，不带入工作目录、执行配置或提示词，不在转换时查询模型。
		 */
		public AgentBean bean() {
			var bean = new AgentBean();
			bean.setBrand(brand);
			bean.setName(name);
			bean.setVer(ver);
			return bean;
		}
	}

	/**
	 * 创建请求；三个角色必须全部省略或全部完整提供，不允许按缺失字段部分回退默认值。
	 */
	public record CreateProjectRequest(String content, String projectName, Model planningAgent, Model developmentAgent, Model reviewAgent) {
		/**
		 * 只有三个模型对象全部为null才表示使用默认选择；部分缺失或空字段不算默认。
		 */
		public boolean defaults() {
			return planningAgent == null && developmentAgent == null && reviewAgent == null;
		}

		/**
		 * 同时校验非空白内容与三角色全给/全省略规则，仅检查输入形状，不访问模型配置或运行资源。
		 */
		public boolean valid() {
			return text(content)
					&& (defaults() || planningAgent != null && planningAgent.valid() && developmentAgent != null
							&& developmentAgent.valid() && reviewAgent != null && reviewAgent.valid());
		}
	}

	/**
	 * 追加需求或提示词的文本输入；实际非空白约束在请求入口检查。
	 */
	public record ProjectContentRequest(String content) {
	}

	/**
	 * 面向HTTP的项目数据；通过view构造时使用不可变列表，不含目录、执行器或提示词引用。
	 */
	public record ProjectResponse(String projectId, String projectName, List<RequirementView> requirements) {
	}

	/**
	 * 需求文本与任务快照，保留模型原文和待确认问题，不引用可变Requirement对象。
	 */
	public record RequirementView(String userContent, String agentUnderstanding, String acceptanceCriteria,
			String userConfirmMsg, List<TaskView> tasks) {
	}

	/**
	 * 规划任务ID、状态和单次结果；id与result.taskId分别代表规划任务和一次执行。
	 */
	public record TaskView(String id, String content, String acceptanceCriteria, TaskStatus status, ResultView result) {
	}

	/**
	 * 执行结果快照；tokenCount为null表示未知，待确认和失败均不等于执行成功。
	 */
	public record ResultView(String taskId, boolean success, String errorMessage, Long tokenCount, String summary,
			boolean confirmationRequired, String confirmationMessage) {
	}

	/** 与领域写入使用同一对象锁复制状态和结果；调用方须先在同一锁内重载以保证持久化状态，本方法不访问数据库。 */
	public static ProjectResponse view(Project project) {
		synchronized (project) {
			return new ProjectResponse(project.getProjectId(), project.getProjectName(),
					project.getRequirements().stream()
							.map(r -> new RequirementView(r.getUserContent(), r.getAgentUnderstanding(),
									r.getAcceptanceCriteria(), r.getUserConfirmMsg(),
									r.getTasks().stream().map(t -> new TaskView(t.getId() == null ? null : t.getId().toString(), t.getContent(),
											t.getAcceptanceCriteria(), t.getStatus(), result(t.getResult()))).toList()))
							.toList());
		}
	}

	/** 检查HTTP文本字段是否非空白，不裁剪原文，也不限制长度或执行内容。 */
	public static boolean text(String value) {
		return value != null && !value.isBlank();
	}

	/**
	 * 复制单次执行结果的可公开字段，null保持为null；不读取执行器或其stderr诊断。
	 */
	private static ResultView result(AgentExecutionResult r) {
		return r == null ? null
				: new ResultView(r.getTaskId(), r.isSuccess(), r.getErrorMessage(), r.getTokenCount(), r.getSummary(),
						r.isConfirmationRequired(), r.getConfirmationMessage());
	}
}
