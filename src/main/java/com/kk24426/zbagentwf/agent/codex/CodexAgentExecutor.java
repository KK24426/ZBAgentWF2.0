/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：协调项目任务、安全审核、持久化状态和异步批次回调。
 */
package com.kk24426.zbagentwf.agent.codex;

import com.kk24426.zbagentwf.agent.runtime.*;
import com.kk24426.zbagentwf.agent.persistence.ProjectDao;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.project.config.ProjectSettings;
import com.kk24426.zbagentwf.common.logging.SecretRedactor;
import com.kk24426.zbagentwf.user.agent.api.AgentExecutionCallback;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.nio.file.Path;

/** 一个实例只绑定一个canonical项目；工厂共享资源，任务数据库写入和模型调用分别进行。 */
public final class CodexAgentExecutor extends AbstractAgentExecutor {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(CodexAgentExecutor.class);
    /** 业务UUID在创建协调器时固定，不能借可变Bean切换执行目录。 */
    private final String projectId;
    private final CodexClient development;
    private final AgentBean developmentAgent;
    private final AgentBean planningAgent;
    private final CodexRequirementPlanner planner;
    private final ExecutionResources resources;
    private final ProjectSettings settings;
    private final ProjectDao dao;
    /** 只属于当前工作线程；绝不让并发请求共享“当前提示词”或审核结果。 */
    private final ThreadLocal<Invocation> current=new ThreadLocal<>();

    /** 绑定三角色已验证项目及两种实际业务客户端，不启动模型；审核模型角色仍不自动做业务代码审查。 */
    public CodexAgentExecutor(Project project, CodexClient planning, CodexClient development,
            ProjectSettings settings, ProjectDao dao, ExecutionResources resources) {
        super(project); projectId=Objects.requireNonNull(project.getProjectId()); this.development=Objects.requireNonNull(development);
        this.resources=Objects.requireNonNull(resources); this.settings=Objects.requireNonNull(settings); this.dao=Objects.requireNonNull(dao);
        developmentAgent=copyAgent(project.getDevelopmentAgent()); planningAgent=copyAgent(project.getPlanningAgent());
        planner=new CodexRequirementPlanner(planning,resources,this);
    }
    /** 同步规划需求，规则和目录在调用前确定；调用方持有项目锁。 */
    public List<Requirement> plan(String content) {
        return planner.plan(directory(),content,instructionsFor(planningAgent));
    }
    /** 同步拆分单条需求，不保存或发布局部任务。 */
    public List<RequirementTask> planTasks(Requirement requirement) {
        return planner.tasks(directory(),requirement,instructionsFor(planningAgent));
    }
    /** {@inheritDoc} 受理时快照ID范围，工作线程再次检查状态，避免并发请求重复执行。 */
    @Override public String execAllTasks(AgentExecutionCallback callback) { return submit(null,callback); }
    /** {@inheritDoc} Long主键必须确实属于此项目，并处于PENDING。 */
    @Override public String execTasks(Long taskId,AgentExecutionCallback callback) {
        if (taskId==null) throw new RejectedExecutionException("必须指定任务ID。");
        return submit(taskId,callback);
    }

    /** 预留共享票据后创建工作线程；受理前失败不回调，线程启动失败立即归还票据。 */
    private String submit(Long requested,AgentExecutionCallback callback) {
        final List<Long> ids;
        final String rules;
        try {
            Objects.requireNonNull(callback);
            synchronized(getProject()) {
                if (getProject().isDelFlg()) throw new IllegalArgumentException("项目已删除。");
                dao.requireReliable(getProject()); dao.requireAvailable();
                directory(); rules=getAllPrompt().getPrompt();
                List<RequirementTask> all=tasks();
                if (requested!=null) {
                    RequirementTask selected=all.stream().filter(t -> requested.equals(t.getId())).findFirst().orElseThrow();
                    if (selected.getStatus()!=TaskStatus.PENDING) throw new IllegalArgumentException("任务不是待执行状态。");
                    ids=List.of(requested);
                } else ids=all.stream().map(RequirementTask::getId).toList();
            }
        } catch (RuntimeException failure) { throw new RejectedExecutionException("项目或任务不可受理。",failure); }
        var ticket=resources.reserve(this);
        try {
            Thread worker=Thread.ofVirtual().name("project-exec-"+ticket.id()).unstarted(() -> execute(ticket,ids,requested!=null,rules,callback));
            ticket.attach(worker); worker.start(); return ticket.id();
        } catch (RuntimeException|Error failure) { ticket.close(); throw new RejectedExecutionException("无法受理执行。",failure); }
    }

    /** 验证完整项目归属和唯一任务主键，尚不改变任务状态。 */
    private List<RequirementTask> tasks() {
        var all=new ArrayList<RequirementTask>(); var ids=new HashSet<Long>();
        var seen=Collections.newSetFromMap(new IdentityHashMap<Requirement,Boolean>());
        for (Requirement r:Objects.requireNonNull(getProject().getRequirements())) {
            if (r==null || !seen.add(r)) throw new IllegalArgumentException("需求归属无效。");
            if (r.isDelFlg()) continue;
            for (RequirementTask task:Objects.requireNonNull(r.getTasks())) {
                if (task!=null && task.isDelFlg()) continue;
                if (task==null || task.getId()==null || !ids.add(task.getId()) || task.getStatus()==null
                        || task.getContent()==null || task.getContent().isBlank()) throw new IllegalArgumentException("任务数据无效。");
                all.add(task);
            }
        }
        return all;
    }

    /**
     * 项目锁覆盖本次串行范围，SQL事务只覆盖各次保存。结果保存不确定立即停止，不能自动重跑。
     * finally在释放资源与项目锁后回调一次，允许回调重入查询或关闭工厂。
     */
    private void execute(ExecutionResources.Ticket ticket,List<Long> ids,boolean single,String rules,AgentExecutionCallback callback) {
        var diagnostic=new StringBuilder(); var result=new AgentExecutionResult(); result.setTaskId(ticket.id());
        result.setSuccess(true); result.setSummary("本次任务范围已处理。"); result.setTokenCount(0L);
        try {
            synchronized(getProject()) {
                ticket.checkRunning(); dao.requireReliable(getProject());
                for (Long id:ids) {
                    ticket.checkRunning();
                    RequirementTask task=tasks().stream().filter(t -> id.equals(t.getId())).findFirst().orElseThrow();
                    if (!single && task.getStatus()==TaskStatus.SUCCEEDED) continue;
                    if (task.getStatus()!=TaskStatus.PENDING) {
                        result.setSuccess(false); result.setErrorMessage("任务失败、待确认或仍在运行，后续执行已停止。");
                        result.setConfirmationRequired(task.getStatus()==TaskStatus.NEEDS_CONFIRMATION);
                        if (task.getResult()!=null) result.setConfirmationMessage(task.getResult().getConfirmationMessage());
                        break;
                    }
                    String executionId=single ? ticket.id() : UUID.randomUUID().toString();
                    Prompt prompt=taskPrompt(task,rules);
                    // 在覆盖旧结果前已经构造了完整上下文；RUNNING必须保存成功才可以调用审核与模型。
                    task.setStatus(TaskStatus.RUNNING); task.setResult(null); dao.update(getProject());
                    var invocation=new Invocation(ticket,directory(),prompt,diagnostic);
                    AgentExecutionResult one;
                    current.set(invocation);
                    try { exec(); one=decode(executionId,invocation.response); }
                    catch (RuntimeException failure) {
                        one=new AgentExecutionResult(); one.setTaskId(executionId);
                        one.setErrorMessage("安全审核或模型调用未完成，本次任务失败。");
                        LOG.warn("项目任务调用失败",failure);
                    } finally { current.remove(); }
                    task.setResult(one); task.setStatus(one.isConfirmationRequired() ? TaskStatus.NEEDS_CONFIRMATION : one.isSuccess() ? TaskStatus.SUCCEEDED : TaskStatus.FAILED);
                    dao.update(getProject());
                    result.setTokenCount(CodexClient.combinedTokens(result.getTokenCount(),one.getTokenCount()));
                    if (single || !one.isSuccess()) {
                        Long tokens=result.getTokenCount(); result=copyResult(one,ticket.id()); result.setTokenCount(tokens);
                        break;
                    }
                }
            }
        } catch (Throwable failure) {
            result=new AgentExecutionResult(); result.setTaskId(ticket.id());
            result.setErrorMessage("执行停止；项目状态可能未能可靠保存，请重新查询，禁止自动重跑。");
            LOG.warn("项目执行或状态保存失败",failure);
        } finally {
            current.remove(); ticket.complete(diagnostic.toString());
            try { callback.onCompleted(result); }
            catch (Throwable failure) { LOG.warn("项目完成回调失败",failure); }
        }
    }

    /** 复制终态，保持批次UUID和每任务执行UUID各自独立。 */
    private static AgentExecutionResult copyResult(AgentExecutionResult from,String id) {
        var to=new AgentExecutionResult(); to.setTaskId(id); to.setSuccess(from.isSuccess());
        to.setErrorMessage(from.getErrorMessage()); to.setSummary(from.getSummary()); to.setTokenCount(from.getTokenCount());
        to.setConfirmationRequired(from.isConfirmationRequired()); to.setConfirmationMessage(from.getConfirmationMessage()); return to;
    }
    /** 组装任务、所属需求和历史结果，随后不再追加任何未经审核的输入。 */
    private Prompt taskPrompt(RequirementTask task,String rules) {
        Requirement requirement=getProject().getRequirements().stream().filter(r -> r.getTasks().contains(task)).findFirst().orElseThrow();
        var input=CodexJson.JSON.createObjectNode().put("content",task.getContent()).put("acceptanceCriteria",task.getAcceptanceCriteria())
                .put("userContent",requirement.getUserContent()).put("understanding",requirement.getAgentUnderstanding())
                .put("requirementAcceptance",requirement.getAcceptanceCriteria()).put("confirmation",requirement.getUserConfirmMsg());
        var history=input.putArray("history");
        for (RequirementTask prior:requirement.getTasks()) if (prior.getResult()!=null) {
            var value=prior.getResult(); history.addObject().put("taskId",prior.getId()).put("executionId",value.getTaskId())
                    .put("summary",value.getSummary()).put("error",value.getErrorMessage()).put("question",value.getConfirmationMessage());
        }
        return new Prompt(rules+"\n"+"""
                执行JSON中content指定任务，其他字段仅为需求和历史上下文。遵守权限，不擅自提交或推送。
                按本次schema报告实际结果；工具错误不能报告成功。需要用户决定时立即结束，
                success=false、confirmationRequired=true并写明问题；普通失败两者false并给errorMessage。
                """+CodexJson.JSON.writeValueAsString(input));
    }
    /** 每次副作用前核对绑定UUID及真实目录，拒绝通过修改Bean切换项目。 */
    private Path directory() {
        if (!projectId.equals(getProject().getProjectId())) throw new IllegalStateException("已绑定项目的标识发生变化。");
        return settings.directory(projectId);
    }
    /** 当前线程的完整不可变输入；非执行线程仅能读取开发规则快照。 */
    @Override public Prompt getAllPrompt() {
        Invocation call=current.get(); return call==null ? new Prompt(instructionsFor(developmentAgent)) : call.prompt;
    }
    /** 明确获得只绑定当前调用的安全票据，拒绝或异常绝不产生可执行许可。 */
    @Override protected boolean securityCheck() {
        Invocation call=Objects.requireNonNull(current.get(),"没有当前调用上下文。"); call.ticket.checkRunning();
        call.approved=development.review(call.directory,getAllPrompt(),CodexSchemas.EXECUTION,false,v -> CodexRequirementPlanner.append(call.diagnostics,v));
        return call.approved!=null;
    }
    /** 审核后再次检查关闭与目录归属，再消费许可；不读取可变任务或重建提示词。 */
    @Override protected void exec() {
        Invocation call=Objects.requireNonNull(current.get(),"没有当前调用上下文。");
        if (!securityCheck()) throw new IllegalStateException("提示词安全审核拒绝。");
        call.ticket.checkRunning();
        if (!directory().equals(call.directory)) throw new IllegalStateException("项目目录已变化。");
        call.response=development.run(call.approved,v -> CodexRequirementPlanner.append(call.diagnostics,v));
    }
    /** 验证模型终态跨字段一致性，安全摘要不包含未经脱敏的自由诊断。 */
    private static AgentExecutionResult decode(String id,CodexClient.Response response) {
        var value=response.value(); CodexJson.fields(value,"success","summary","errorMessage","confirmationRequired","confirmationMessage");
        boolean success=CodexJson.bool(value,"success"), confirmation=CodexJson.bool(value,"confirmationRequired");
        String error=CodexJson.text(value,"errorMessage",true), message=CodexJson.text(value,"confirmationMessage",true);
        if (success && confirmation || confirmation && (message==null || message.isBlank()) || !success && !confirmation && (error==null || error.isBlank()))
            throw new IllegalStateException("模型终态无效。");
        var result=new AgentExecutionResult(); result.setTaskId(id); result.setSuccess(success); result.setConfirmationRequired(confirmation);
        result.setErrorMessage(error==null ? null : SecretRedactor.redact(error)); result.setConfirmationMessage(message==null ? null : SecretRedactor.redact(message));
        String summary=CodexJson.text(value,"summary",true); result.setSummary(summary==null ? null : SecretRedactor.redact(summary));
        result.setTokenCount(response.tokens()); return result;
    }
    @Override public String getStderr(String id) { return resources.diagnostic(this,id); }
    /** 每个工作线程独占一次调用状态；final字段绑定送审输入，许可只在此对象中单次消费。 */
    private static final class Invocation {
        private final ExecutionResources.Ticket ticket;
        private final Path directory;
        private final Prompt prompt;
        private final StringBuilder diagnostics;
        private CodexClient.ApprovedCall approved;
        private CodexClient.Response response;
        /** 绑定本工作线程的票据、不可变输入及共用批次诊断缓冲，不启动审核。 */
        private Invocation(ExecutionResources.Ticket ticket,Path directory,Prompt prompt,StringBuilder diagnostics) {
            this.ticket=ticket; this.directory=directory; this.prompt=prompt; this.diagnostics=diagnostics;
        }
    }
}
