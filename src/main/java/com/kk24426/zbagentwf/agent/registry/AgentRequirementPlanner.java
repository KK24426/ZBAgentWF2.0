/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：将项目已绑定的规划执行器关联到对应的只读规划适配器。
 */
package com.kk24426.zbagentwf.agent.registry;

import com.kk24426.zbagentwf.common.project.bean.Project;
import com.kk24426.zbagentwf.common.project.bean.Requirement;
import java.util.List;
import java.util.Objects;

/** 实现层组合服务，不新增 user 业务接口，也不从执行摘要猜测规划数据。 */
public final class AgentRequirementPlanner {
    private final AgentExecFactoryImpl factory;
    public AgentRequirementPlanner(AgentExecFactoryImpl factory) { this.factory = Objects.requireNonNull(factory); }
    public List<Requirement> plan(Project project, String content) {
        return factory.plannerFor(project.getPlanningAgent()).plan(project.getWorkingDirectory(), content);
    }
}
