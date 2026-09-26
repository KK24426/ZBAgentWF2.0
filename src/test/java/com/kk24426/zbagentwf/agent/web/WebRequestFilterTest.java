/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：验证请求日志边界、固定错误响应与日志故障处理。
 */
package com.kk24426.zbagentwf.agent.web;

import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.ErrorStatus;
import com.kk24426.zbagentwf.common.logging.LogFailureMonitor;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import jakarta.servlet.ServletException;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class WebRequestFilterTest {
    @TempDir Path temp;
    private MsgCatalog messages;
    private WebRequestFilter filter;

    @BeforeEach
    void prepare() throws Exception {
        messages = MsgCatalog.load(temp);
        filter = new WebRequestFilter(messages, new MsgLocaleResolver("auto"));
    }

    @Test
    void everyFixedRejectionUsesRequestLanguageAndKeepsItsHttpStatus() throws Exception {
        for (String language : MsgCatalog.LANGUAGES) {
            for (String[] scenario : new String[][]{
                    {"GET", "/unknown", "404", "http.notFound"},
                    {"GET", "/api/chat", "405", "http.methodNotAllowed"},
                    {"POST", "/api/chat", "415", "http.jsonRequired"}}) {
                var request = new MockHttpServletRequest(scenario[0], scenario[1]);
                request.addHeader("Accept-Language", language);
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (a, b) -> fail("应在过滤器拒绝"));
                assertEquals(Integer.parseInt(scenario[2]), response.getStatus());
                assertEquals(messages.get(scenario[3], Locale.forLanguageTag(language)), response.getContentAsString());
                assertEquals(language, response.getHeader("Content-Language"));
            }
            var request = new MockHttpServletRequest("GET", "/");
            request.addHeader("Accept-Language", language);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (a, b) -> { throw new ServletException("private-error"); });
            assertEquals(500, response.getStatus());
            assertEquals(messages.get("http.internalError", Locale.forLanguageTag(language)), response.getContentAsString());
        }
    }

    @Test
    void malformedLanguageHeaderDoesNotChangeRejectionsOrLogFailureProtection() throws Exception {
        for (String[] scenario : new String[][]{
                {"GET", "/unknown", "404"}, {"GET", "/api/chat", "405"}, {"POST", "/api/chat", "415"}}) {
            var request = new MockHttpServletRequest(scenario[0], scenario[1]);
            request.addHeader("Accept-Language", "en;q=broken");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (a, b) -> fail("应保持原错误"));
            assertEquals(Integer.parseInt(scenario[2]), response.getStatus());
            assertEquals("zh-CN", response.getHeader("Content-Language"));
        }
        try {
            new LogFailureMonitor().addStatusEvent(new ErrorStatus("test", this));
            for (String language : new String[]{"en", "ja", "en;q=broken"}) {
                var request = new MockHttpServletRequest("GET", "/");
                request.addHeader("Accept-Language", language);
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (a, b) -> fail("日志故障不得继续"));
                assertEquals(503, response.getStatus());
                Locale locale = Locale.forLanguageTag(language.contains(";") ? "zh-CN" : language);
                assertEquals(messages.get("http.unavailable", locale), response.getContentAsString());
            }
        } finally {
            LogFailureMonitor.reset();
        }
    }

    @Test
    void onlyExactChatPostAndStaticScriptAreAllowedAndMdcIsRestored() throws Exception {
        MDC.put("route", "previous-route");
        MDC.put("requestId", "previous-request");
        try {
            var request = new MockHttpServletRequest("POST", "/api/chat");
            request.setContentType("application/json;charset=UTF-8");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (a, b) -> {
                assertEquals("CHAT", MDC.get("route"));
                assertNotEquals("previous-request", MDC.get("requestId"));
            });
            assertEquals(200, response.getStatus());
            assertEquals("previous-route", MDC.get("route"));
            assertEquals("previous-request", MDC.get("requestId"));
            assertTrue(response.getHeader("Content-Security-Policy").contains("script-src 'self'"));
            assertTrue(response.getHeader("Content-Security-Policy").contains("connect-src 'self'"));
            for (String method : new String[]{"GET", "HEAD"}) {
                filter.doFilter(new MockHttpServletRequest(method, "/chat.js"),
                        new MockHttpServletResponse(), (a, b) -> assertEquals("ASSET", MDC.get("route")));
            }
            for (String path : new String[]{"/", "/api/chat/", "/api/other", "/chat.js"}) {
                var blocked = new MockHttpServletResponse();
                filter.doFilter(new MockHttpServletRequest("POST", path), blocked,
                        (a, b) -> fail("非聊天 POST 不得进入下游"));
                assertEquals(405, blocked.getStatus());
            }
            var head = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("HEAD", "/api/chat"), head,
                    (a, b) -> fail("聊天只接受 POST"));
            assertEquals(405, head.getStatus());
            assertEquals("POST", head.getHeader("Allow"));
            assertEquals("", head.getContentAsString());
        } finally {
            MDC.remove("route");
            MDC.remove("requestId");
        }
    }

    @Test
    void unsupportedChatMediaHasFixedResponseAndFailureRestoresRoute() throws Exception {
        for (String media : new String[]{null, "text/plain", "application/xml", "application/json/broken"}) {
            var request = new MockHttpServletRequest("POST", "/api/chat");
            request.setContentType(media);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (a, b) -> fail("拒绝非 JSON"));
            assertEquals(415, response.getStatus());
            assertEquals("仅支持 JSON 请求。", response.getContentAsString());
            assertNull(MDC.get("route"));
        }
        var request = new MockHttpServletRequest("POST", "/api/chat");
        request.setContentType("application/json");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response,
                (a, b) -> { throw new ServletException("普通隐私标记"); });
        assertEquals(500, response.getStatus());
        assertNull(MDC.get("route"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void rejectsUnknownPathsAndMethodsWithoutLoggingInputs() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(WebRequestFilter.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> records = new ListAppender<>();
        records.start();
        logger.addAppender(records);
        try {
            for (String method : new String[]{"GET", "PRIVATE-METHOD"}) {
                var request = new MockHttpServletRequest(method, "/private-path");
                request.setQueryString("secret=private-query");
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (a, b) -> fail("不得进入下游"));
                assertEquals(method.equals("GET") ? 404 : 405, response.getStatus());
                assertFalse(response.getContentAsString().contains("private"));
                assertNotNull(response.getHeader("X-Request-ID"));
                assertNull(MDC.get("requestId"));
            }
            assertEquals(2, records.list.size());
            assertTrue(records.list.stream().noneMatch(e -> e.getFormattedMessage().contains("private")
                    || e.getFormattedMessage().contains("PRIVATE")));
        } finally {
            logger.detachAppender(records);
            logger.setLevel(previousLevel);
            records.stop();
        }
    }

    @Test
    void hidesExceptionsAndRestoresRequestContext() throws Exception {
        MDC.put("requestId", "previous");
        try {
            var response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", "/"), response,
                    (a, b) -> { throw new ServletException("token=private-value", new IllegalStateException("cause")); });
            assertEquals(500, response.getStatus());
            assertFalse(response.getContentAsString().contains("private-value"));
            assertFalse(response.getContentAsString().contains("ServletException"));
            assertEquals("previous", MDC.get("requestId"));
        } finally {
            MDC.remove("requestId");
        }
    }

    @Test
    void failedLoggingRejectsNewRequestsAndHeadHasNoBody() throws Exception {
        try {
            new LogFailureMonitor().addStatusEvent(new ErrorStatus("test", this));
            var response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("HEAD", "/"), response,
                    (a, b) -> fail("日志失败后不执行下游"));
            assertEquals(503, response.getStatus());
            assertEquals("", response.getContentAsString());
        } finally {
            LogFailureMonitor.reset();
        }
    }
}
