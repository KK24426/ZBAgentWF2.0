package com.kk24426.zbagentwf.common.agent.bean;

/** Agent的执行结果 */
public class AgentExecResult {
	/** 执行结果，false失败 true成功 */
	private boolean isSuccess;
	/** 失败的时候记录失败原因 */
	private String errMsg;
	/** 本次执行消耗多少token */
	private Long token;
	/** 本次执行的汇报、总结 */
	private String execRes;
	/** 是否有用户确认事项 */
	private boolean isConfirm;
	/** 用户确认事项 */
	private boolean confirmMsg;
}
