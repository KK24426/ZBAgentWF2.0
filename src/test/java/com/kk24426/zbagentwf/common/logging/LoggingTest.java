/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：验证脱敏、完整异常链、目录隔离、滚动和日志故障。
 */
package com.kk24426.zbagentwf.common.logging;

import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.status.ErrorStatus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoggingTest {
    @TempDir Path temp;

    @Test
    void chatDiagnosticsHideFreeTextWithAndWithoutHttpContextButKeepNormalLogs() {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        SanitizingEncoder encoder = encoder(context);
        try {
            var failure = new IllegalStateException("普通隐私标记", new IllegalArgumentException("普通隐私原因"));
            failure.addSuppressed(new RuntimeException("普通隐私附加"));
            for (String logger : new String[]{"com.kk24426.zbagentwf.user.chat.service.ChatService",
                    "com.kk24426.zbagentwf.agent.chat.AgentChatImpl", "org.springframework.web.Binding",
                    "com.kk24426.zbagentwf.agent.codex.CodexClient",
                    "com.kk24426.zbagentwf.agent.registry.AgentExecFactoryImpl",
                    "com.kk24426.zbagentwf.agent.runtime.ExecutionResources",
                    "com.kk24426.zbagentwf.agent.project.ProjectUserifImpl",
                    "com.kk24426.zbagentwf.agent.web.WebRequestFilter"}) {
                var event = new LoggingEvent("test", context.getLogger(logger), Level.ERROR,
                        "普通隐私消息", failure, null);
                event.setMDCPropertyMap(logger.contains(".chat.") || logger.contains(".codex.") || logger.contains(".project.") ? java.util.Map.of()
                        : java.util.Map.of("route", "CHAT"));
                String output = new String(encoder.encode(event), StandardCharsets.UTF_8);
                assertFalse(output.contains("普通隐私"));
                assertTrue(output.contains("IllegalStateException"));
                assertTrue(output.contains("Caused by:"));
                assertTrue(output.contains("Suppressed:"));
                assertTrue(output.contains("LoggingTest.java"));
            }
            var binding = new LoggingEvent("test", context.getLogger("org.springframework.web.Binding"),
                    Level.WARN, "普通隐私绑定诊断", null, null);
            binding.setMDCPropertyMap(java.util.Map.of("route", "CHAT"));
            assertFalse(new String(encoder.encode(binding), StandardCharsets.UTF_8).contains("普通隐私"));
            for (String logger : new String[]{"com.kk24426.zbagentwf.ZbAgentWfApplication",
                    "com.kk24426.zbagentwf.agent.persistence.Database", "com.kk24426.zbagentwf.agent.web.WebRequestFilter"}) {
                var event = new LoggingEvent("test", context.getLogger(logger), Level.INFO,
                        "固定运行摘要 route=CHAT", null, null);
                event.setMDCPropertyMap(java.util.Map.of("route", "CHAT"));
                assertTrue(new String(encoder.encode(event), StandardCharsets.UTF_8).contains("固定运行摘要 route=CHAT"));
            }
        } finally {
            encoder.stop();
            context.stop();
        }
    }

    @Test
    void protocolDiagnosticsKeepChainButNeverEchoRawRequestData() {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        SanitizingEncoder encoder = encoder(context);
        try {
            var failure = new IllegalArgumentException("private-target",
                    new IllegalStateException("private-query"));
            failure.addSuppressed(new RuntimeException("private-header"));
            for (String name : new String[]{"org.apache.coyote.http11.Http11Processor",
                    "org.apache.tomcat.util.http.parser.Cookie"}) {
                var event = new LoggingEvent("test", context.getLogger(name), Level.INFO,
                        "private-method", failure, new Object[]{"private-argument"});
                String text = new String(encoder.encode(event), StandardCharsets.UTF_8);
                assertFalse(text.contains("private-"));
                assertTrue(text.contains("HTTP 容器诊断"));
                assertTrue(text.contains("IllegalArgumentException"));
                assertTrue(text.contains("Caused by:"));
                assertTrue(text.contains("Suppressed:"));
                assertTrue(text.contains("LoggingTest.java"));
            }
        } finally {
            encoder.stop();
            context.stop();
        }
    }

    @Test
    void completeExceptionChainAndMessagesAreRedacted() {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        SanitizingEncoder encoder = encoder(context);
        var cause = new IllegalArgumentException("password=hidden-password");
        var failure = new IllegalStateException("token=hidden-token", cause);
        failure.addSuppressed(new RuntimeException("username=hidden-user"));
        var event = new LoggingEvent("test", context.getLogger("test"), Level.ERROR,
                "中文诊断 Authorization: Bearer hidden-bearer", failure, null);
        String text = new String(encoder.encode(event), StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(text.contains("中文诊断")),
                () -> assertTrue(text.contains("IllegalStateException")),
                () -> assertTrue(text.contains("Caused by:")),
                () -> assertTrue(text.contains("Suppressed:")),
                () -> assertTrue(text.contains("LoggingTest.java")),
                () -> assertFalse(text.contains("hidden-")),
                () -> assertFalse(SecretRedactor.redact("jdbc:mysql://host/db?password=bad").contains("host/db")));
        encoder.stop();
        context.stop();
    }

    @Test
    void createsUniqueRunDirectoriesAndRejectsFileAsRoot() throws Exception {
        String previous = System.getProperty("zb.log-dir");
        try {
            System.setProperty("zb.log-dir", temp.toString());
            Path first = RunLogging.prepare();
            Path second = RunLogging.prepare();
            assertNotEquals(first.getParent(), second.getParent());
            assertTrue(Files.exists(first));
            System.setProperty("zb.log-dir", first.toString());
            assertThrows(java.io.IOException.class, RunLogging::prepare);
        } finally {
            if (previous == null) System.clearProperty("zb.log-dir"); else System.setProperty("zb.log-dir", previous);
        }
    }

    @Test
    void rollingArchivesAndFinalRecordSurviveStop() throws Exception {
        LoggerContext context = new LoggerContext();
        context.setName("rolling-test");
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        context.start();
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("file");
        Path file = temp.resolve("application.log");
        appender.setFile(file.toString());
        appender.setEncoder(encoder(context));
        var policy = new SizeAndTimeBasedRollingPolicy<ILoggingEvent>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern(file + ".%d{yyyy-MM-dd}.%i.gz");
        policy.setMaxFileSize(FileSize.valueOf("1KB"));
        policy.setMaxHistory(0);
        policy.start();
        appender.setRollingPolicy(policy);
        appender.start();
        assertTrue(appender.isStarted());
        var logger = context.getLogger("rolling");
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
        for (int i = 0; i < 100; i++) logger.debug("中文 {}", "x".repeat(1024));
        logger.info("FINAL-RECORD");
        appender.stop();
        context.stop();
        assertTrue(Files.readString(file).contains("FINAL-RECORD"),
                () -> context.getStatusManager().getCopyOfStatusList().toString());
        try (var files = Files.list(temp)) {
            var archives = files.filter(p -> p.toString().endsWith(".gz")).toList();
            assertFalse(archives.isEmpty());
            try (var input = new GZIPInputStream(Files.newInputStream(archives.getFirst()))) {
                assertTrue(new String(input.readAllBytes(), StandardCharsets.UTF_8).contains("中文"));
            }
        }
    }

    @Test
    void runtimeLoggingFailureIsObservable() {
        LogFailureMonitor.reset();
        var originalError = System.err;
        var bytes = new java.io.ByteArrayOutputStream();
        try (var error = new java.io.PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setErr(error);
            new LogFailureMonitor().addStatusEvent(new ErrorStatus("private failure text", this));
            assertTrue(LogFailureMonitor.hasFailed());
            assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("日志系统故障"));
            assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("private failure text"));
        } finally {
            System.setErr(originalError);
            LogFailureMonitor.reset();
        }
    }

    private SanitizingEncoder encoder(LoggerContext context) {
        SanitizingEncoder encoder = new SanitizingEncoder();
        encoder.setContext(context);
        encoder.setPattern("%level %msg%n%ex{full}");
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();
        return encoder;
    }
}
