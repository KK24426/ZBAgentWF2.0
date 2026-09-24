package com.kk24426.zbagentwf.common.project.bean;

import java.util.List;

/**
 * 用户需求类
 */
public class Requirement {

	/** 用户原始需求 */
	private String userContent;

	/** Agent梳理后的理解 */
	private String AgentPrepare;

	/** 验收标准 */
	private String Accept;

	/** 可执行Task */
	private List<RequirementTask> requirementTask;

	/** 用户确认事项 */
	private String userConfirmMsg;
}
