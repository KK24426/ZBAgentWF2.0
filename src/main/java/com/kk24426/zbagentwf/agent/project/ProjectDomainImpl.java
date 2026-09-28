/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：协调项目目录、两阶段规划、完整持久化和canonical对象。
 */
package com.kk24426.zbagentwf.agent.project;

import com.kk24426.zbagentwf.agent.registry.*;
import com.kk24426.zbagentwf.agent.persistence.ProjectDao;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.project.config.ProjectSettings;
import com.kk24426.zbagentwf.user.project.api.ProjectDomain;
import com.kk24426.zbagentwf.user.agent.api.AgentExecutorFactory;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** 数据库为事实来源；缓存只稳定运行时对象身份，读取恢复来自一致事务快照。 */
public final class ProjectDomainImpl extends ProjectDomain {
    private final ProjectSettings settings;
    private final AgentExecutorFactory factory;
    private final AgentRequirementPlanner planner;
    private final ProjectDao store;
    private final RoleAgentResolver roles;
    /** 仅保存已完整持久化的项目，不作为数据库不可用时的降级存储。 */
    private final ConcurrentHashMap<String,Project> projects=new ConcurrentHashMap<>();
    /** 注入已装配依赖；构造不读取项目、创建目录或调用模型。 */
    public ProjectDomainImpl(ProjectSettings settings,AgentExecutorFactory factory,AgentRequirementPlanner planner,ProjectDao store,RoleAgentResolver roles) {
        super(store); this.settings=Objects.requireNonNull(settings); this.factory=Objects.requireNonNull(factory);
        this.planner=Objects.requireNonNull(planner); this.store=Objects.requireNonNull(store); this.roles=Objects.requireNonNull(roles);
    }
    /**
     * 三角色和数据库可用后才创建root/local/UUID。规划与拆任务全部成功后一次保存。
     * 进入持久化后的异常保留目录，避免提交结果不确定时删掉已保存项目的目录。
     */
    @Override public Project newProject(String content,String projectName,AgentBean planning,AgentBean development,AgentBean review) {
        requireContent(content); store.requireAvailable();
        Project project=new Project(projectName,new ArrayList<>(),roles.bind(planning,AgentRole.PLANNING),
                roles.bind(development,AgentRole.DEVELOPMENT),roles.bind(review,AgentRole.REVIEW));
        project.setProjectId(UUID.randomUUID().toString()); factory.getExecutor(project);
        Path created=null; boolean persistAttempted=false;
        try {
            created=settings.create(project.getProjectId());
            synchronized(project) {
                project.setRequirements(createRequiremensOnNew(project,content));
                persistAttempted=true; dao.insert(project);
                if (projects.putIfAbsent(project.getProjectId(),project)!=null) throw new IllegalStateException("项目标识重复。");
            }
            return project;
        } catch (IOException|RuntimeException failure) {
            if (!persistAttempted && created!=null) {
                try { Files.delete(created); } catch(IOException cleanup) { failure.addSuppressed(cleanup); }
            }
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("项目目录创建失败。",failure);
        }
    }
    @Override protected AgentBean getAgent(AgentRole role) { return roles.defaultFor(role); }
    /** 生成完整暂存需求；待确认项不拆任务，任一步失败都不追加部分数据。 */
    @Override protected List<Requirement> createRequiremensOnNew(Project project,String content) { return planned(project,content); }
    /** 先规划需求再逐条拆分，所有结果仍属局部列表，不进入事务或项目集合。 */
    private List<Requirement> planned(Project project,String content) {
        requireContent(content); List<Requirement> added=planner.plan(project,content);
        for (Requirement r:added) if (r.getUserConfirmMsg()==null || r.getUserConfirmMsg().isBlank())
            r.setTasks(planner.tasks(project,r));
        return added;
    }
    /** 完整规划成功后合并保存；保存失败还原列表并保留DAO不确定标志，必须重载后继续。 */
    @Override public List<Requirement> createRequiremens(Project project,String content) {
        synchronized(project) {
            requireCanonical(project); store.requireReliable(project); settings.directory(project.getProjectId());
            List<Requirement> added=planned(project,content); List<Requirement> old=project.getRequirements();
            var all=new ArrayList<>(old); all.addAll(added); project.setRequirements(all);
            try { dao.update(project); } catch(RuntimeException failure) { project.setRequirements(old); throw failure; }
            return new ArrayList<>(added);
        }
    }
    /** 为本项目尚无任务且信息充分的需求拆分并保存；禁止覆写已有任务。 */
    @Override public List<RequirementTask> createRequirementsTask(Project project,Requirement requirement) {
        synchronized(project) {
            requireCanonical(project); store.requireReliable(project);
            if (project.getRequirements().stream().noneMatch(r -> r==requirement)) throw new IllegalArgumentException("需求不属于项目。");
            var added=planner.tasks(project,requirement); var old=requirement.getTasks(); requirement.setTasks(added);
            try { dao.update(project); } catch(RuntimeException failure) { requirement.setTasks(old); throw failure; }
            return new ArrayList<>(added);
        }
    }
    /** 追加规则后短事务保存，不立刻调用模型；下一次真实调用仍需审核完整输入。 */
    @Override public void addProjectPrompt(String projectId,String content) {
        requireContent(content); Project project=getProject(projectId);
        synchronized(project) {
            store.requireReliable(project); Prompt old=project.getProjectPrompt();
            project.setProjectPrompt(new Prompt(old==null || old.getPrompt()==null ? content : old.getPrompt()+"\n"+content));
            try { dao.update(project); } catch(RuntimeException failure) { project.setProjectPrompt(old); throw failure; }
        }
    }
    /**
     * 原子取得canonical对象后在同一对象锁内一致读重载，避免跨版本子表和多份执行器。
     * 未知或逻辑删除项目不从目录重建；数据库不可用不能返回旧缓存假装成功。
     */
    @Override public Project getProject(String projectId) {
        settings.path(projectId);
        Project project=projects.computeIfAbsent(projectId,id -> required(store.findByProjectId(id)));
        synchronized(project) { store.refresh(project,required(store.findByProjectId(projectId))); return project; }
    }
    /** 异步批次由worker持项目锁；本线程等待期间不占该锁，避免Controller等待形成死锁。 */
    @Override public List<Requirement> execTask(Project project) {
        requireCanonical(project);
        var done=new CompletableFuture<AgentExecutionResult>();
        factory.getExecutor(project).execAllTasks(done::complete);
        boolean interrupted=false;
        try {
            for (;;) {
                try { done.get(); break; }
                catch(InterruptedException failure) { interrupted=true; }
                catch(ExecutionException failure) { throw new IllegalStateException("任务结果不可用。",failure); }
            }
            synchronized(project) { store.requireReliable(project); return new ArrayList<>(project.getRequirements()); }
        } finally { if(interrupted) Thread.currentThread().interrupt(); }
    }
    /** 只接受当前实现登记的持久化对象，不能把同ID的外部对象作为执行目标。 */
    private void requireCanonical(Project project) {
        if (project==null || projects.get(project.getProjectId())!=project) throw new IllegalArgumentException("项目不属于当前服务。");
    }
    /** 查询不到时保留既有未知项目语义，不用空项目替代数据库记录。 */
    private static Project required(Project project) { if(project==null) throw new IllegalArgumentException("项目不存在。"); return project; }
    /** 在目录、数据库写入和模型调用之前拒绝空白业务输入。 */
    private static void requireContent(String content) { if(content==null || content.isBlank()) throw new IllegalArgumentException("内容不能为空。"); }
}
