/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保留用户定义的本机 Agent 发现、查询和刷新抽象能力。
 */
package com.kk24426.zbagentwf.user.agent.userif;

import java.util.List;

import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 本类包含Agent最基础的一些功能
 *
 */
public abstract class AgentBase implements UserInterface {

	/**
	 * 获取当前本机所有可用的Agent工具并存入全局变量中， 在可用Agent的时候重新获取存在时返回已存在的list， 这个方法应该在初始化的时候调用一次。
	 * 沒有可用模型时应该返回业务错误
	 *
	 * @return 本机所有可用的Agent工具
	 */
	public abstract List<AgentBean> getActiveAgent();

	/**
	 * 通过指定品牌和模型名去全局变量中获取特定的Agent
	 *
	 * @param brand 提供方
	 * @param name 模型名
	 * @param ver 版本号
	 * @return Agent工具
	 */
	public abstract AgentBean getActiveAgent(String brand, String name, String ver);

	/**
	 * 刷新并返回当前本机所有可用的Agent工具并更新全局变量
	 *
	 * @return 刷新后的本机 Agent 工具列表
	 */
	public abstract List<AgentBean> refreshAgentList();

}
