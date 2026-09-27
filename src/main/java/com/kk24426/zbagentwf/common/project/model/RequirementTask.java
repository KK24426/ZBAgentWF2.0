/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存需求拆分出的规划任务、状态与执行结果。
 */
package com.kk24426.zbagentwf.common.project.model;

import com.kk24426.zbagentwf.common.DataBean;
import com.kk24426.zbagentwf.common.agent.model.AgentExecutionResult;

/** 保存需求拆分出的规划任务、状态与执行结果。 普通属性原样存取，不自动执行业务校验。 */
public class RequirementTask extends DataBean {

	/** 交由 Agent 执行的任务内容。 */
	private String content;

	/** 本任务的验收标准。 */
	private String acceptanceCriteria;

	/** 任务当前状态；本类不自动推进状态。 */
	private TaskStatus status = TaskStatus.PENDING;

	/** 关联的单次执行结果；尚无结果时为 null。 */
	private AgentExecutionResult result;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAcceptanceCriteria() {
		return acceptanceCriteria;
	}

	public void setAcceptanceCriteria(String acceptanceCriteria) {
		this.acceptanceCriteria = acceptanceCriteria;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public AgentExecutionResult getResult() {
		return result;
	}

	public void setResult(AgentExecutionResult result) {
		this.result = result;
	}
}
