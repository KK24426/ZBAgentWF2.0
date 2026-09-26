/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：通过真实 Controller 和 Service 加测试替身验证聊天 HTTP 契约。
 */
package com.kk24426.zbagentwf.user.chat.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.kk24426.zbagentwf.agent.web.WebRequestFilter;
import com.kk24426.zbagentwf.agent.web.MsgLocaleResolver;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import com.kk24426.zbagentwf.common.chat.ChatRequest;
import com.kk24426.zbagentwf.common.exception.AgentUnavailableException;
import com.kk24426.zbagentwf.common.logging.SanitizingEncoder;
import com.kk24426.zbagentwf.user.chat.service.ChatAgentFixture;
import com.kk24426.zbagentwf.user.chat.service.ChatService;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class ChatControllerTest {
    private final ChatAgentFixture agent = new ChatAgentFixture();
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;
    @TempDir Path temp;
    private MsgCatalog messages;

    @BeforeEach
    void prepare() throws Exception {
        messages = MsgCatalog.load(temp);
        var locales = new MsgLocaleResolver("auto");
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(new ChatService(agent), messages))
                .setLocaleResolver(locales).addFilters(new WebRequestFilter(messages, locales)).build();
    }

    @Test
    void localizedErrorResponsesKeepStatusShapeAndManualPreference() throws Exception {
        for (String language : MsgCatalog.LANGUAGES) {
            Locale locale = Locale.forLanguageTag(language);
            mvc.perform(post("/api/chat").header("Accept-Language", language)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(messages.get("http.chat.invalid", locale)));
            mvc.perform(post("/api/chat").header("Accept-Language", language)
                            .contentType(MediaType.APPLICATION_JSON).content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(messages.get("http.chat.invalid", locale)));
            agent.failure = new AgentUnavailableException();
            mvc.perform(post("/api/chat").header("Accept-Language", language)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"你好\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(messages.get("http.chat.unavailable", locale)));
            agent.failure = new IllegalStateException("private-input");
            mvc.perform(post("/api/chat").header("Accept-Language", language)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"你好\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(messages.get("http.internalError", locale)));
        }
        mvc.perform(post("/api/chat").header("Accept-Language", "en").cookie(new Cookie("zb.locale", "ja"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(header().string("Content-Language", "ja"))
                .andExpect(content().string(messages.get("http.chat.invalid", Locale.JAPANESE)));
        agent.failure = null;
        mvc.perform(post("/api/chat").header("Accept-Language", "ja")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"原样保留\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reply").value(agent.result));
        assertEquals("原样保留", agent.message);
    }

    @Test
    void chineseMultilineMessageAndReplyArePreserved() throws Exception {
        String message = "  你好\n第二行  ";
        var response = mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ChatRequest(message))))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reply").value(agent.result)).andReturn().getResponse();
        assertEquals(message, agent.message);
        assertEquals(1, agent.calls);
        assertNotNull(response.getHeader("X-Request-ID"));
        assertNull(MDC.get("route"));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ChatRequest("a".repeat(4000)))))
                .andExpect(status().isOk());
    }

    @Test
    void invalidJsonTypesAndInputBoundsFailBeforeAgent() throws Exception {
        for (String body : new String[]{"", "null", "{}", "{\"message\":null}", "{\"message\":\"\"}",
                "{\"message\":\" \\n\\t\"}", "{\"message\":123}", "{\"message\":true}",
                "{\"message\":[]}", "{\"message\":{}}", "[]", "{\"message\":普通隐私标记}",
                json.writeValueAsString(new ChatRequest("a".repeat(4001)))}) {
            mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("请求无效：message 必须是非空白字符串，且长度不能超过 4000。"));
        }
        assertEquals(0, agent.calls);
    }

    @Test
    void unsupportedMediaAndMethodDoNotReachAgent() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.TEXT_PLAIN).content("普通隐私标记"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/chat")).andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", "POST"));
        mvc.perform(post("/api/other").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        assertEquals(0, agent.calls);
    }

    @Test
    void unavailableAndUnexpectedAgentFailuresHaveFixedResponses() throws Exception {
        agent.failure = new AgentUnavailableException();
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"你好\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Agent 尚未接入，暂时无法生成回复。"));
        agent.failure = new IllegalArgumentException("普通隐私标记");
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"你好\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("服务内部错误，请联系维护者并提供请求编号。"));
    }

    @Test
    void malformedJsonAndProviderExceptionChainsNeverLeakNaturalLanguage() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ChatController.class);
        Level previous = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        var bytes = new ByteArrayOutputStream();
        var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        var encoder = new SanitizingEncoder();
        encoder.setContext(context);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern("%level %logger [requestId=%X{requestId}] %msg%n%ex{full}");
        encoder.start();
        var appender = new OutputStreamAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setOutputStream(bytes);
        appender.start();
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        logger.addAppender(appender);
        try {
            mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":普通隐私标记}"))
                    .andExpect(status().isBadRequest());
            agent.failure = new IllegalStateException("普通隐私标记", new IllegalArgumentException("普通隐私原因"));
            agent.failure.addSuppressed(new RuntimeException("普通隐私附加信息"));
            var response = mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"普通隐私输入\"}"))
                    .andExpect(status().isInternalServerError()).andReturn().getResponse();
            String log = bytes.toString(StandardCharsets.UTF_8);
            assertFalse(log.contains("普通隐私"));
            assertTrue(log.contains("HttpMessageNotReadableException"));
            assertTrue(log.contains("IllegalStateException"));
            assertTrue(log.contains("Caused by:"));
            assertTrue(log.contains("Suppressed:"));
            assertTrue(log.contains("ChatControllerTest.java"));
            assertTrue(log.contains(response.getHeader("X-Request-ID")));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
            logger.setAdditive(previousAdditive);
            appender.stop();
            encoder.stop();
        }
    }
}
