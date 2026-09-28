/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.5
 * 功能概要：接收项目操作，通过服务查找数据库项目并返回安全快照及本地化错误。
 */
package com.kk24426.zbagentwf.agent.project.controller;

import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.common.project.dto.ProjectHttp;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import com.kk24426.zbagentwf.user.project.service.ProjectService;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

/** 所有后续请求通过 ID 查找服务器内对象；不会反序列化客户端 Project。 */
@RestController
public class ProjectController {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectController.class);
    private final ProjectService projectService;
    private final MsgCatalog messages;

    /** 绑定领域服务和三语消息目录；构造不查询项目或启动模型。 */
    public ProjectController(ProjectService projectService, MsgCatalog messages) {
        this.projectService = projectService;
        this.messages = messages;
    }

    /**
     * 校验目标文本及完整三角色选择后创建并规划项目，成功返回201快照；全部省略角色时才采用默认绑定。
     */
    @PostMapping(value = "/api/projects", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody ProjectHttp.CreateProjectRequest request, Locale locale) {
        if (request == null || !request.valid()) return invalid(locale);
        Project project = request.defaults() ? projectService.newProject(request.content(), request.projectName())
                : projectService.newProject(request.content(), request.projectName(), request.planningAgent().bean(),
                        request.developmentAgent().bean(), request.reviewAgent().bean());
        return ResponseEntity.status(201).body(snapshot(project));
    }

    /**
     * 按ID从数据库重载并返回项目安全快照；未知ID为404，不从目录恢复项目，不返回运行时引用。
     */
    @GetMapping(value = "/api/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@PathVariable String projectId, Locale locale) {
        return withProject(projectId, locale, p -> ResponseEntity.ok(snapshot(p)));
    }

    /**
     * 校验并追加后续调用使用的提示词，成功返回204；不在响应中回传已保存规则。
     */
    @PostMapping(value = "/api/projects/{projectId}/prompts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> prompt(@PathVariable String projectId, @RequestBody ProjectHttp.ProjectContentRequest request, Locale locale) {
        if (request == null || !ProjectHttp.text(request.content())) return invalid(locale);
        return withProject(projectId, locale, p -> {
            projectService.addProjectPrompt(p, request.content());
            return ResponseEntity.noContent().build();
        });
    }

    /**
     * 对非空白新增需求调用规划并返回更新后的完整快照；规划本身不执行开发任务。
     */
    @PostMapping(value = "/api/projects/{projectId}/requirements", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> requirements(@PathVariable String projectId, @RequestBody ProjectHttp.ProjectContentRequest request, Locale locale) {
        if (request == null || !ProjectHttp.text(request.content())) return invalid(locale);
        return withProject(projectId, locale, p -> {
            projectService.createRequirements(p, request.content());
            return ResponseEntity.ok(snapshot(p));
        });
    }

    /**
     * 仅接受空JSON对象，等待本次任务执行结束并返回快照；200不代表所有任务成功，异常前也可能已有任务完成。
     */
    @PostMapping(value = "/api/projects/{projectId}/tasks/execute", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(@PathVariable String projectId, @RequestBody Map<String, Object> request, Locale locale) {
        if (request == null || !request.isEmpty()) return invalid(locale);
        return withProject(projectId, locale, p -> {
            projectService.execTask(projectId);
            return ResponseEntity.ok(snapshot(p));
        });
    }

    /**
     * 先按ID查找；领域/执行器负责写锁，响应另行原子重载并复制；只把初次查找时的IllegalArgumentException映射404，内部操作错误继续上抛。
     */
    private ResponseEntity<?> withProject(String id, Locale locale, Function<Project, ResponseEntity<?>> operation) {
        if (!ProjectHttp.text(id)) return invalid(locale);
        Project project;
        try { project = projectService.getProject(id); }
        catch (IllegalArgumentException missing) { return error(404, "http.project.notFound", locale); }
        // 不能持有项目锁等待异步worker；否则worker无法取得同一锁完成回调。
        return operation.apply(project);
    }

    /**
     * 在canonical对象锁内重载并复制响应，避免初次查找后另一请求保存失败而泄露未提交终态。
     * 重载失败继续上抛，不回退旧快照；此处不等待异步执行，序列化在释放锁后使用不可变副本。
     */
    private ProjectHttp.ProjectResponse snapshot(Project project) {
        synchronized (project) {
            return ProjectHttp.view(projectService.getProject(project.getProjectId()));
        }
    }

    /**
     * 将反序列化失败映射固定400，原异常仅交给脱敏日志链，不向客户端暴露请求正文。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> malformed(HttpMessageNotReadableException failure, Locale locale) {
        LOG.warn("项目请求正文无效", failure);
        return invalid(locale);
    }

    /**
     * 将配置未就绪和受理额度拒绝映射503；此状态不承诺前面的任务均未执行，调用方须重新查询。
     */
    @ExceptionHandler({AgentConfigurationUnavailableException.class, RejectedExecutionException.class})
    public ResponseEntity<String> unavailable(RuntimeException failure, Locale locale) {
        LOG.warn("项目 Agent 暂不可用", failure);
        return error(503, "http.project.unavailable", locale);
    }

    /**
     * 将其余运行异常映射固定500并保留脱敏异常链，避免把内部参数异常当成客户端输入错误。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> failed(RuntimeException failure, Locale locale) {
        LOG.error("项目操作失败", failure);
        return error(500, "http.internalError", locale);
    }

    private ResponseEntity<String> invalid(Locale locale) { return error(400, "http.project.invalid", locale); }
    /**
     * 用固定消息key和当前请求语言构造纯文本错误，设置Content-Language，不拼入异常原文。
     */
    private ResponseEntity<String> error(int status, String key, Locale locale) {
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .header("Content-Language", MsgCatalog.languageTag(locale)).body(messages.get(key, locale));
    }
}
