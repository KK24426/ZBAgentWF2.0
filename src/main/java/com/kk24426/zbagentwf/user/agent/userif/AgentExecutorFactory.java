/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按 Agent 实体获取独立绑定的执行器。
 */
package com.kk24426.zbagentwf.user.agent.userif;

import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 执行器构建工厂
 */
public interface AgentExecutorFactory extends UserInterface {
	/** 同一未修改的 Agent 实体复用实例，不同实体独立；由工厂管理关闭，不支持的模型明确失败，不自动回退。 */
	AgentExecutor getExecutor(AgentBean agent);
}
