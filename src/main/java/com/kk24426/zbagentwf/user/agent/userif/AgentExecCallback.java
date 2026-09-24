package com.kk24426.zbagentwf.user.agent.userif;

import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;

/**
 * Agent执行结果的回调类
 */
public interface AgentExecCallback {
	public void agentExecCallback(AgentExecResult agentExecResult);
}
