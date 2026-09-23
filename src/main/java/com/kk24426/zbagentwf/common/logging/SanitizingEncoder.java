/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：对消息和异常完整渲染结果统一脱敏后编码。
 */
package com.kk24426.zbagentwf.common.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import java.util.Arrays;

/** 避免只处理 %msg 而遗漏 %ex 中的凭据；保留异常类型和调用链。 */
public class SanitizingEncoder extends PatternLayoutEncoder {
    @Override
    public byte[] encode(ILoggingEvent event) {
        // 容器解析失败发生在 Servlet Filter 前；异常原文可能携带任意路径、查询或请求头。
        // 对协议解析组件保留来源、级别和完整调用链，但消息只使用固定摘要，不能靠凭据关键词猜测。
        String logger = event.getLoggerName();
        boolean protocol = logger.startsWith("org.apache.coyote.")
                || logger.startsWith("org.apache.tomcat.util.http.");
        boolean chatRoute = "CHAT".equals(event.getMDCPropertyMap().get("route"));
        boolean chatComponent = logger.startsWith("com.kk24426.zbagentwf.user.chat.")
                || logger.startsWith("com.kk24426.zbagentwf.agent.chat.");
        // 聊天异常和 Spring 绑定诊断可能含任意自然语言输入，不能仅按凭据关键词脱敏。
        boolean chatDiagnostic = (chatRoute || chatComponent) && event.getThrowableProxy() != null
                || chatRoute && logger.startsWith("org.springframework.");
        if (protocol || chatDiagnostic) {
            IThrowableProxy safeThrowable = wrap(event.getThrowableProxy());
            LoggingEvent safe = new LoggingEvent() {
                @Override public IThrowableProxy getThrowableProxy() { return safeThrowable; }
            };
            safe.setLoggerName(event.getLoggerName());
            safe.setLevel(event.getLevel());
            safe.setInstant(event.getInstant());
            safe.setThreadName(event.getThreadName());
            safe.setMDCPropertyMap(event.getMDCPropertyMap());
            safe.setLoggerContextRemoteView(event.getLoggerContextVO());
            safe.setMessage(protocol ? "HTTP 容器诊断：原始协议内容已隐藏。"
                    : "聊天请求诊断：原始内容已隐藏。");
            event = safe;
        }
        return SecretRedactor.redact(getLayout().doLayout(event)).getBytes(getCharset());
    }

    private static IThrowableProxy wrap(IThrowableProxy value) {
        return value == null ? null : new SafeThrowable(value);
    }

    /** 屏蔽请求异常自由文本，异常类型、堆栈及 cause/suppressed 关系保持不变。 */
    private record SafeThrowable(IThrowableProxy original) implements IThrowableProxy {
        public String getMessage() { return "[原始内容已隐藏]"; }
        public String getClassName() { return original.getClassName(); }
        public StackTraceElementProxy[] getStackTraceElementProxyArray() { return original.getStackTraceElementProxyArray(); }
        public int getCommonFrames() { return original.getCommonFrames(); }
        public IThrowableProxy getCause() { return wrap(original.getCause()); }
        public IThrowableProxy[] getSuppressed() {
            var suppressed = original.getSuppressed();
            return suppressed == null ? null : Arrays.stream(suppressed).map(SanitizingEncoder::wrap).toArray(IThrowableProxy[]::new);
        }
        public boolean isCyclic() { return original.isCyclic(); }
    }
}
