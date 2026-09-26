package com.kk24426.zbagentwf.user.agent.userif;

import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 执行器构建工厂
 */
public interface AgentExecFactory extends UserInterface {
	AgentExecutor getExecutor(AgentBean agent);
}
