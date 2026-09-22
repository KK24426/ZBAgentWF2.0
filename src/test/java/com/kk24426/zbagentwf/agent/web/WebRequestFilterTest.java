/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.1
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
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class WebRequestFilterTest {
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
                new WebRequestFilter().doFilter(request, response, (a, b) -> fail("不得进入下游"));
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
            new WebRequestFilter().doFilter(new MockHttpServletRequest("GET", "/"), response,
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
            new WebRequestFilter().doFilter(new MockHttpServletRequest("HEAD", "/"), response,
                    (a, b) -> fail("日志失败后不执行下游"));
            assertEquals(503, response.getStatus());
            assertEquals("", response.getContentAsString());
        } finally {
            LogFailureMonitor.reset();
        }
    }
}
