/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.4
 * 功能概要：接收项目操作，通过服务查找内存项目并返回安全快照及本地化错误。
 */
package com.kk24426.zbagentwf.user.project.controller;

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

    public ProjectController(ProjectService projectService, MsgCatalog messages) {
        this.projectService = projectService;
        this.messages = messages;
    }

    @PostMapping(value = "/api/projects", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody ProjectHttp.CreateProjectRequest request, Locale locale) {
        if (request == null || !request.valid()) return invalid(locale);
        Project project = request.defaults() ? projectService.newProject(request.content())
                : projectService.newProject(request.content(), request.planningAgent().bean(),
                        request.developmentAgent().bean(), request.reviewAgent().bean());
        return ResponseEntity.status(201).body(ProjectHttp.view(project));
    }

    @GetMapping(value = "/api/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@PathVariable String projectId, Locale locale) {
        return withProject(projectId, locale, p -> ResponseEntity.ok(ProjectHttp.view(p)));
    }

    @PostMapping(value = "/api/projects/{projectId}/prompts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> prompt(@PathVariable String projectId, @RequestBody ProjectHttp.ProjectContentRequest request, Locale locale) {
        if (request == null || !ProjectHttp.text(request.content())) return invalid(locale);
        return withProject(projectId, locale, p -> {
            projectService.addProjectPrompt(p, request.content());
            return ResponseEntity.noContent().build();
        });
    }

    @PostMapping(value = "/api/projects/{projectId}/requirements", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> requirements(@PathVariable String projectId, @RequestBody ProjectHttp.ProjectContentRequest request, Locale locale) {
        if (request == null || !ProjectHttp.text(request.content())) return invalid(locale);
        return withProject(projectId, locale, p -> {
            projectService.createRequirements(p, request.content());
            return ResponseEntity.ok(ProjectHttp.view(p));
        });
    }

    @PostMapping(value = "/api/projects/{projectId}/tasks/execute", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(@PathVariable String projectId, @RequestBody Map<String, Object> request, Locale locale) {
        if (request == null || !request.isEmpty()) return invalid(locale);
        return withProject(projectId, locale, p -> {
            projectService.execTask(projectId);
            return ResponseEntity.ok(ProjectHttp.view(p));
        });
    }

    private ResponseEntity<?> withProject(String id, Locale locale, Function<Project, ResponseEntity<?>> operation) {
        if (!ProjectHttp.text(id)) return invalid(locale);
        Project project;
        try { project = projectService.getProject(id); }
        catch (IllegalArgumentException missing) { return error(404, "http.project.notFound", locale); }
        // 查询和操作/复制同一项目串行；不同项目可以独立处理。
        synchronized (project) { return operation.apply(project); }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> malformed(HttpMessageNotReadableException failure, Locale locale) {
        LOG.warn("项目请求正文无效", failure);
        return invalid(locale);
    }

    @ExceptionHandler({AgentConfigurationUnavailableException.class, RejectedExecutionException.class})
    public ResponseEntity<String> unavailable(RuntimeException failure, Locale locale) {
        LOG.warn("项目 Agent 暂不可用", failure);
        return error(503, "http.project.unavailable", locale);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> failed(RuntimeException failure, Locale locale) {
        LOG.error("项目操作失败", failure);
        return error(500, "http.internalError", locale);
    }

    private ResponseEntity<String> invalid(Locale locale) { return error(400, "http.project.invalid", locale); }
    private ResponseEntity<String> error(int status, String key, Locale locale) {
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .header("Content-Language", MsgCatalog.languageTag(locale)).body(messages.get(key, locale));
    }
}
