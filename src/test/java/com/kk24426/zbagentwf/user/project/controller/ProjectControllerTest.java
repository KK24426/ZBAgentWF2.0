/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证项目 Controller、Service、精确路由及三语隐私边界。
 */
package com.kk24426.zbagentwf.user.project.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kk24426.zbagentwf.agent.web.*;
import com.kk24426.zbagentwf.common.agent.bean.*;
import com.kk24426.zbagentwf.common.project.bean.*;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import com.kk24426.zbagentwf.common.logging.SanitizingEncoder;
import com.kk24426.zbagentwf.user.project.domain.ProjectDomain;
import com.kk24426.zbagentwf.user.project.service.ProjectService;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;

class ProjectControllerTest {
    @TempDir Path temp;
    private final ProjectDomain domain = mock(ProjectDomain.class);
    private MockMvc mvc;
    private MsgCatalog messages;
    private Project project;

    @BeforeEach void prepare() throws Exception {
        messages = MsgCatalog.load(temp);
        var locales = new MsgLocaleResolver("auto");
        mvc = MockMvcBuilders.standaloneSetup(new ProjectController(new ProjectService(domain), messages))
                .setLocaleResolver(locales).addFilters(new WebRequestFilter(messages, locales)).build();
        project = new Project(); project.setProjectId("project-1"); project.setWorkingDirectory(temp.resolve("private-directory"));
        var secret = new Prompt(); secret.setPrompt("private-rules"); project.setProjectPrompt(secret);
        var r = new Requirement(); r.setUserContent("需求");
        var t = new RequirementTask(); t.setId("task-1"); t.setContent("实现"); r.getTasks().add(t);
        project.getRequirements().add(r);
        when(domain.newProject(anyString())).thenReturn(project);
        when(domain.newProject(anyString(), any(), any(), any())).thenReturn(project);
        when(domain.getProject("project-1")).thenReturn(project);
        when(domain.getProject("missing")).thenThrow(new IllegalArgumentException("internal-private-text"));
        when(domain.execTask(project)).thenAnswer(call -> {
            var result = new AgentExecutionResult(); result.setTaskId("execution-1"); result.setSuccess(true); result.setSummary("完成");
            t.setResult(result); t.setStatus(TaskStatus.SUCCEEDED); return project.getRequirements();
        });
    }

    @Test void allFiveOperationsUseServerProjectAndReturnOnlySnapshots() throws Exception {
        String created = mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"原始需求\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.projectId").value("project-1"))
                .andExpect(jsonPath("$.workingDirectory").doesNotExist())
                .andExpect(jsonPath("$.planningAgent").doesNotExist())
                .andExpect(jsonPath("$.projectPrompt").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertFalse(created.contains("private-directory")); assertFalse(created.contains("private-rules"));
        verify(domain).newProject("原始需求");
        mvc.perform(get("/api/projects/project-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].tasks[0].status").value("PENDING"));
        mvc.perform(post("/api/projects/project-1/prompts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"新增规则\"}")).andExpect(status().isNoContent());
        verify(domain).addProjectPrompt("project-1", "新增规则");
        mvc.perform(post("/api/projects/project-1/requirements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"新增需求\"}")).andExpect(status().isOk());
        verify(domain).createRequirements(same(project), eq("新增需求"));
        mvc.perform(post("/api/projects/project-1/tasks/execute").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].tasks[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.requirements[0].tasks[0].result.taskId").value("execution-1"));
        verify(domain).execTask(same(project));
    }

    @Test void explicitModelsOverrideDefaultsAndPartialSelectionIsRejected() throws Exception {
        String model = "{\"brand\":\"p\",\"name\":\"m\",\"ver\":\"v\"}";
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"任务\",\"planningAgent\":" + model + ",\"developmentAgent\":" + model
                        + ",\"reviewAgent\":" + model + "}"))
                .andExpect(status().isCreated());
        verify(domain, never()).newProject(anyString());
        var captor = org.mockito.ArgumentCaptor.forClass(AgentBean.class);
        verify(domain).newProject(eq("任务"), captor.capture(), captor.capture(), captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(a -> a.getBrand().equals("p") && a.getVer().equals("v")));
        clearInvocations(domain);
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"任务\",\"planningAgent\":" + model + "}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(domain);
    }

    @Test void invalidMissingUnavailableAndInternalErrorsKeepDistinctStatusAndLocalization() throws Exception {
        for (String language : MsgCatalog.LANGUAGES) {
            Locale locale = Locale.forLanguageTag(language);
            for (String input : List.of("{", "{}", "{\"content\":\"  \"}")) {
                mvc.perform(post("/api/projects").header("Accept-Language", language)
                        .contentType(MediaType.APPLICATION_JSON).content(input))
                        .andExpect(status().isBadRequest()).andExpect(content().string(messages.get("http.project.invalid", locale)));
            }
            mvc.perform(get("/api/projects/missing").header("Accept-Language", language))
                    .andExpect(status().isNotFound()).andExpect(content().string(messages.get("http.project.notFound", locale)));
            doThrow(new AgentConfigurationUnavailableException()).when(domain).newProject("unavailable");
            mvc.perform(post("/api/projects").header("Accept-Language", language)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"unavailable\"}"))
                    .andExpect(status().isServiceUnavailable()).andExpect(content().string(messages.get("http.project.unavailable", locale)));
        }
        when(domain.newProject("internal")).thenThrow(new IllegalArgumentException("private-arbitrary-text"));
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"internal\"}"))
                .andExpect(status().isInternalServerError()).andExpect(content().string(messages.get("http.internalError", Locale.CHINESE)));
        when(domain.execTask(project)).thenThrow(new RejectedExecutionException("private-resource-details"));
        mvc.perform(post("/api/projects/project-1/tasks/execute").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test void onlyExactRoutesAndMethodsAreExposed() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", "POST"));
        mvc.perform(post("/api/projects/project-1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/projects/project-1/tasks/execute")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/projects/project-1/unapproved")).andExpect(status().isNotFound());
        mvc.perform(post("/api/projects").contentType(MediaType.TEXT_PLAIN).content("private-input"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/projects/project-1/tasks/execute").contentType(MediaType.APPLICATION_JSON).content("{\"directory\":\"outside\"}"))
                .andExpect(status().isBadRequest());
        verify(domain, never()).execTask(any());
    }

    @Test void projectExceptionLogsHideFreeTextButPreserveCauseAndStack() throws Exception {
        var bytes = new ByteArrayOutputStream();
        var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        var encoder = new SanitizingEncoder(); encoder.setContext(context); encoder.setPattern("%msg%n%ex");
        encoder.setCharset(StandardCharsets.UTF_8); encoder.start();
        var appender = new OutputStreamAppender<ILoggingEvent>(); appender.setContext(context);
        appender.setEncoder(encoder); appender.setOutputStream(bytes); appender.start();
        var logger = (Logger) LoggerFactory.getLogger(ProjectController.class);
        var previousLevel = logger.getLevel(); boolean previousAdditive = logger.isAdditive();
        logger.setLevel(ch.qos.logback.classic.Level.WARN); logger.setAdditive(false); logger.addAppender(appender);
        try {
            doAnswer(call -> { throw new IllegalStateException("private-natural-input",
                    new IllegalArgumentException("private-cause")); }).when(domain).newProject("private-natural-input");
            mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"private-natural-input\"}")).andExpect(status().isInternalServerError());
        } finally { logger.detachAppender(appender); logger.setLevel(previousLevel); logger.setAdditive(previousAdditive); appender.stop(); encoder.stop(); }
        String log = bytes.toString(StandardCharsets.UTF_8);
        assertFalse(log.contains("private-natural-input")); assertFalse(log.contains("private-cause"));
        assertTrue(log.contains("IllegalStateException")); assertTrue(log.contains("Caused by:"));
        assertTrue(log.contains("ProjectController.java"));
    }
}
