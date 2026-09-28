/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：协调一个项目的模型；工厂拥有生命周期，业务调用方不关闭共享实例。
 */
package com.kk24426.zbagentwf.user.agent.api;

import com.kk24426.zbagentwf.common.agent.model.Prompt;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.user.UserInterface;
import java.util.Objects;

/** 协调一个项目的模型；工厂拥有生命周期，业务调用方不关闭共享实例。 */
public abstract class ProjectAgentExecutor implements UserInterface {
    private final Project project;
    private final Prompt userPrompt;
    /** 绑定项目，不读取配置、创建目录或调用模型。 */
    public ProjectAgentExecutor(Project project) { this(project, null); }
    /** 绑定项目和可选不可变用户规则；构造不调用子类方法。 */
    public ProjectAgentExecutor(Project project, Prompt userPrompt) {
        this.project = Objects.requireNonNull(project);
        this.userPrompt = userPrompt;
    }
    protected Project getProject() { return project; }
    protected Prompt getUserPrompt() { return userPrompt; }
    /** 读取显式初始化的通用规则，未就绪时失败，getter不读文件。 */
    protected abstract Prompt getDefaultPrompt();
    /** 读取已初始化安全规则，getter不触发模型审核。 */
    protected abstract Prompt getSecurityPrompt();
    /** 执行本线程调用上下文的最终输入；必须先审核，不使用实例级共享当前任务。 */
    protected abstract void exec();
    /**
     * 异步执行受理时选定的任务范围，按需求/任务顺序串行；失败、待确认或遗留RUNNING停止。
     * 返回批次UUID，最终回调一次且result.taskId为该UUID；各任务另保存独立执行结果。
     * 受理失败同步抛RejectedExecutionException且不回调；文件修改不随保存失败回滚。
     */
    public abstract String execAllTasks(AgentExecutionCallback callback);
    /**
     * 异步执行本项目指定PENDING任务；taskId是数据库Long主键，不是执行UUID。
     * 返回单次执行UUID，最终回调一次；无效归属/状态、关闭或满额在受理前拒绝且不回调。
     */
    public abstract String execTasks(Long taskId, AgentExecutionCallback callback);
    /**
     * 查询本实例返回的执行或批次UUID的脱敏诊断，批次汇总限64KiB。
     * 未知、其他归属或过期返回null；完成诊断共享256条/30分钟，不是持久任务结果。
     */
    public abstract String getStderr(String taskId);
    /** 审核本线程不可变调用快照；失败、上下文缺失或非法返回不得放行。 */
    protected abstract boolean securityCheck();
    /** 调用线程返回最终输入；调用外返回项目开发规则快照，不读文件或运行模型。 */
    public abstract Prompt getAllPrompt();
}
