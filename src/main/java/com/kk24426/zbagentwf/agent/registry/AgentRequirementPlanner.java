/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：通过项目协调器组合需求规划和任务拆分。
 */
package com.kk24426.zbagentwf.agent.registry;
import com.kk24426.zbagentwf.common.project.model.*;
import java.util.*;
/** 技术组合服务，不新增用户业务入口；规划数据只在完整成功后由上层发布。 */
public final class AgentRequirementPlanner {
    private final AgentExecutorFactoryImpl factory;
    /** 保存共享工厂引用，构造不调用模型。 */
    public AgentRequirementPlanner(AgentExecutorFactoryImpl factory) { this.factory=Objects.requireNonNull(factory); }
    /** 使用项目规划模型同步生成需求列表，不直接落库。 */
    public List<Requirement> plan(Project project,String content) { return factory.coordinator(project).plan(content); }
    /** 使用同一规划模型拆分单条明确需求，不覆盖已保存任务。 */
    public List<RequirementTask> tasks(Project project,Requirement requirement) { return factory.coordinator(project).planTasks(requirement); }
}
