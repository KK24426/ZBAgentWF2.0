package com.kk24426.zbagentwf.user.agent.userif;

import java.io.OutputStream;

import com.kk24426.zbagentwf.common.agent.bean.AgentBean;

/**
 * Agent执行类
 */
public abstract class AgentExec {

	/** 使用的模型 */
	private AgentBean agent;

	public AgentExec(AgentBean agent) {
		super();
		this.agent = agent;
	}

	/**
	 * 通过cli或者调用sdk等方式使用传入的模型执行用户指令
	 * 
	 * @param content           用户输入的指令
	 * @param memory            之前的记忆，没有可以留空
	 * @param agentExecCallback 回调函数
	 * @return
	 */
	public abstract Integer exec(String content, String memory, AgentExecCallback agentExecCallback);

	/**
	 * 
	 * @return 返回模型执行过程中的信息
	 */
	public abstract String getStderr();

}
