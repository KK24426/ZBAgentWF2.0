/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
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

	public record Model(String brand, String name, String ver) {
		public boolean valid() {
			return text(brand) && text(name) && text(ver);
		}

		public AgentBean bean() {
			var bean = new AgentBean();
			bean.setBrand(brand);
			bean.setName(name);
			bean.setVer(ver);
			return bean;
		}
	}

	public record CreateProjectRequest(String content, Model planningAgent, Model developmentAgent, Model reviewAgent) {
		public boolean defaults() {
			return planningAgent == null && developmentAgent == null && reviewAgent == null;
		}

		public boolean valid() {
			return text(content)
					&& (defaults() || planningAgent != null && planningAgent.valid() && developmentAgent != null
							&& developmentAgent.valid() && reviewAgent != null && reviewAgent.valid());
		}
	}

	public record ProjectContentRequest(String content) {
	}

	public record ProjectResponse(String projectId, List<RequirementView> requirements) {
	}

	public record RequirementView(String userContent, String agentUnderstanding, String acceptanceCriteria,
			String userConfirmMsg, List<TaskView> tasks) {
	}

	public record TaskView(String id, String content, String acceptanceCriteria, TaskStatus status, ResultView result) {
	}

	public record ResultView(String taskId, boolean success, String errorMessage, Long tokenCount, String summary,
			boolean confirmationRequired, String confirmationMessage) {
	}

	/** 与领域写入使用同一对象锁，一次复制状态和结果；之后序列化不读取可变业务对象。 */
	public static ProjectResponse view(Project project) {
		synchronized (project) {
			return new ProjectResponse(project.getProjectId(),
					project.getRequirements().stream()
							.map(r -> new RequirementView(r.getUserContent(), r.getAgentUnderstanding(),
									r.getAcceptanceCriteria(), r.getUserConfirmMsg(),
									r.getTasks().stream().map(t -> new TaskView(t.getId(), t.getContent(),
											t.getAcceptanceCriteria(), t.getStatus(), result(t.getResult()))).toList()))
							.toList());
		}
	}

	public static boolean text(String value) {
		return value != null && !value.isBlank();
	}

	private static ResultView result(AgentExecutionResult r) {
		return r == null ? null
				: new ResultView(r.getTaskId(), r.isSuccess(), r.getErrorMessage(), r.getTokenCount(), r.getSummary(),
						r.isConfirmationRequired(), r.getConfirmationMessage());
	}
}
