package com.kk24426.zbagentwf.user.project.userif;

import java.util.List;

import com.kk24426.zbagentwf.common.project.bean.Requirement;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 提供项目功能相关能力类
 * 
 */
public abstract class ProjectUserif implements UserInterface {

	/**
	 * 根据用户的输入去新建项目
	 * 
	 * @param content
	 */
	public abstract void newProject(String content);

	/**
	 * 根据用户的需求执行需求规划，把大需求拆分成最适合Agent执行的Task。<br/>
	 * 当需求过大的时候，可以把大需求拆分成小需求，然后再拆成子Task。
	 * 
	 * @param content 用户需求
	 * @return
	 */
	public abstract List<Requirement> createrRequirement(String content);

	/**
	 * 执行用户需求，并把所有task的状态返回
	 * 
	 * @param requirements
	 */
	public abstract List<Requirement> execTask(List<Requirement> requirements);

}
