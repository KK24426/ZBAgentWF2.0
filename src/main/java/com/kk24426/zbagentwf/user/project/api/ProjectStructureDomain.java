/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义按项目查询当前应用、模块和具体功能的抽象契约。
 */
package com.kk24426.zbagentwf.user.project.api;

import java.util.List;

import com.kk24426.zbagentwf.common.project.model.ProjectApplication;
import com.kk24426.zbagentwf.common.project.model.ProjectFunction;
import com.kk24426.zbagentwf.common.project.model.ProjectModule;
import com.kk24426.zbagentwf.user.UserInterface;

/**
 * 当前项目结构的只读查询契约；本轮没有实现或 Spring 装配，不查询待实施计划。
 * 以下均为后续实现要求：仅返回指定项目内未删除的对象，返回值与内部可变对象分离；
 * 未找到或归属不匹配时，单项返回 null，列表返回空列表，列表按 id 升序。
 * projectId 始终为 Project.id 的 Long 数据库主键，不是 Project.projectId 字符串 UUID。
 */
public abstract class ProjectStructureDomain implements UserInterface {

    /**
     * 查询项目当前拥有的应用端，不修改结构或触发规划。
     * @param projectId 所属 Project.id 数据库主键
     * @return 当前应用的独立数据列表，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectApplication> listApplications(Long projectId);

    /**
     * 在项目范围内查询单个应用。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectApplicationId 目标 ProjectApplication.id 数据库主键
     * @return 应用的独立数据对象；不存在、已删除或归属不匹配时为 null
     */
    public abstract ProjectApplication getApplication(Long projectId, Long projectApplicationId);

    /**
     * 查询指定应用的单层模块；应用必须属于指定项目。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectApplicationId 父级 ProjectApplication.id 数据库主键
     * @return 当前模块的独立数据列表，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectModule> listModules(Long projectId, Long projectApplicationId);

    /**
     * 沿应用归属校验后查询单个模块，不允许跨项目读取。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectModuleId 目标 ProjectModule.id 数据库主键
     * @return 模块的独立数据对象；不存在、已删除或归属不匹配时为 null
     */
    public abstract ProjectModule getModule(Long projectId, Long projectModuleId);

    /**
     * 查询指定模块的具体功能；模块及其应用必须属于指定项目。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectModuleId 父级 ProjectModule.id 数据库主键
     * @return 当前功能的独立数据列表，按 id 升序，无结果时为空列表
     */
    public abstract List<ProjectFunction> listFunctions(Long projectId, Long projectModuleId);

    /**
     * 沿模块和应用归属校验后查询单个具体功能。
     * @param projectId 所属 Project.id 数据库主键
     * @param projectFunctionId 目标 ProjectFunction.id 数据库主键
     * @return 功能的独立数据对象；不存在、已删除或归属不匹配时为 null
     */
    public abstract ProjectFunction getFunction(Long projectId, Long projectFunctionId);
}
