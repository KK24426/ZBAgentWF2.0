/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按项目实体获取协调三角色模型的执行器。
 */
package com.kk24426.zbagentwf.user.agent.api;

import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 按项目绑定模型协调器，生命周期由工厂拥有；不以模型相同合并项目上下文
 */
public interface AgentExecutorFactory extends UserInterface {
	/** 同一项目实体复用协调器，项目的三个模型绑定后不得变更；由工厂管理关闭，不支持的模型明确失败，不自动回退。 */
	ProjectAgentExecutor getExecutor(Project project);
}
