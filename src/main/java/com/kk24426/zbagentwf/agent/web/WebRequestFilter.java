/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：限制首页、聊天和项目路由访问面，记录脱敏请求诊断并隐藏错误详情。
 */
package com.kk24426.zbagentwf.agent.web;

import com.kk24426.zbagentwf.common.logging.LogFailureMonitor;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 仅开放固定资源、聊天及已批准的项目路由；其它入口仍需用户批准。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebRequestFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(WebRequestFilter.class);
    private static final Set<String> PATHS = Set.of("/", "/index.html", "/app.css", "/favicon.svg", "/chat.js",
            "/projects", "/projects.html", "/projects.css", "/projects.js", "/messages.js");
    private final MsgCatalog messages;
    private final MsgLocaleResolver locales;

    public WebRequestFilter(MsgCatalog messages, MsgLocaleResolver locales) {
        this.messages = messages;
        this.locales = locales;
    }

    /**
     * 对精确路由执行方法及JSON校验，附加安全响应头、请求编号和固定错误提示。
     * 日志只使用固定路由类别；失败响应已提交时继续抛异常，结束时恢复进入前的MDC，避免请求线程复用污染。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String previous = MDC.get("requestId");
        String previousRoute = MDC.get("route");
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        long started = System.nanoTime();
        String method = Set.of("GET", "HEAD", "POST").contains(request.getMethod()) ? request.getMethod() : "OTHER";
        String path = request.getRequestURI();
        boolean chat = "/api/chat".equals(path);
        boolean projectCollection = "/api/projects".equals(path);
        boolean projectItem = path.matches("/api/projects/[A-Za-z0-9-]+");
        boolean projectAction = path.matches("/api/projects/[A-Za-z0-9-]+/(prompts|requirements|tasks/execute)");
        boolean project = projectCollection || projectItem || projectAction;
        boolean write = chat || projectCollection || projectAction;
        String route = project ? "PROJECT" : chat ? "CHAT" : Set.of("/", "/index.html", "/projects", "/projects.html").contains(path) ? "HOME"
                : PATHS.contains(path) ? "ASSET" : "OTHER";
        MDC.put("route", route);
        Locale locale = locales.resolveLocale(request);
        try {
            response.setHeader("X-Request-ID", requestId);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Content-Security-Policy",
                    "default-src 'none'; style-src 'self'; img-src 'self'; script-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'");
            response.setHeader("Cache-Control", "no-store");
            if (LogFailureMonitor.hasFailed()) {
                reject(response, 503, "http.unavailable", method, locale);
            } else if (write ? !method.equals("POST") : !Set.of("GET", "HEAD").contains(method)) {
                response.setHeader("Allow", write ? "POST" : "GET, HEAD");
                reject(response, 405, "http.methodNotAllowed", method, locale);
            } else if (!chat && !project && !PATHS.contains(path)) {
                reject(response, 404, "http.notFound", method, locale);
            } else if (write && !isJson(request.getContentType())) {
                // consumes 匹配失败时 Controller 尚未选中，在精确路由边界返回固定错误。
                reject(response, 415, "http.jsonRequired", method, locale);
            } else {
                chain.doFilter(request, response);
            }
        } catch (IOException | ServletException | RuntimeException failure) {
            LOG.error("HTTP 请求执行失败 route={}", route, failure);
            if (!response.isCommitted()) {
                response.resetBuffer();
                reject(response, 500, "http.internalError", method, locale);
            } else {
                // 已发送的响应不可重写，保留异常及请求编号，不把细节作为二次响应写出。
                throw failure;
            }
        } finally {
            LOG.info("HTTP 请求完成 method={} route={} status={} elapsedMs={}", method, route,
                    response.getStatus(), (System.nanoTime() - started) / 1_000_000);
            if (previous == null) MDC.remove("requestId"); else MDC.put("requestId", previous);
            if (previousRoute == null) MDC.remove("route"); else MDC.put("route", previousRoute);
        }
    }

    /**
     * 仅接受application/json（可带合法参数）；缺失或格式非法返回false，由请求边界统一映射415。
     */
    private static boolean isJson(String contentType) {
        if (contentType == null) return false;
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return "application".equalsIgnoreCase(mediaType.getType())
                    && "json".equalsIgnoreCase(mediaType.getSubtype());
        } catch (InvalidMediaTypeException invalid) {
            return false;
        }
    }

    /**
     * 写入固定状态和本地化纯文本，不暴露内部异常；HEAD只写响应头，不输出正文。
     */
    private void reject(HttpServletResponse response, int status, String key, String method, Locale locale)
            throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Content-Language", MsgCatalog.languageTag(locale));
        if (!method.equals("HEAD")) response.getWriter().write(messages.get(key, locale));
    }
}
