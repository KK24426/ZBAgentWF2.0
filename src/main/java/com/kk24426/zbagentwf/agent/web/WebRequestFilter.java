/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：限制首页和聊天路由访问面，记录脱敏请求诊断并隐藏错误详情。
 */
package com.kk24426.zbagentwf.agent.web;

import com.kk24426.zbagentwf.common.logging.LogFailureMonitor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
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

/** 仅开放固定资源及单次聊天路由；其它入口仍需用户批准。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebRequestFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(WebRequestFilter.class);
    private static final Set<String> PATHS = Set.of("/", "/index.html", "/app.css", "/favicon.svg", "/chat.js");

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
        String route = chat ? "CHAT" : Set.of("/", "/index.html").contains(path) ? "HOME"
                : PATHS.contains(path) ? "ASSET" : "OTHER";
        MDC.put("route", route);
        try {
            response.setHeader("X-Request-ID", requestId);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Content-Security-Policy",
                    "default-src 'none'; style-src 'self'; img-src 'self'; script-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'");
            response.setHeader("Cache-Control", "no-store");
            if (LogFailureMonitor.hasFailed()) {
                reject(response, 503, "服务暂不可用。", method);
            } else if (chat ? !method.equals("POST") : !Set.of("GET", "HEAD").contains(method)) {
                response.setHeader("Allow", chat ? "POST" : "GET, HEAD");
                reject(response, 405, "不支持的请求方式。", method);
            } else if (!chat && !PATHS.contains(path)) {
                reject(response, 404, "页面不存在。", method);
            } else if (chat && !isJson(request.getContentType())) {
                // consumes 匹配失败时 Controller 尚未选中，在精确路由边界返回固定错误。
                reject(response, 415, "仅支持 JSON 请求。", method);
            } else {
                chain.doFilter(request, response);
            }
        } catch (IOException | ServletException | RuntimeException failure) {
            LOG.error("HTTP 请求执行失败 route={}", route, failure);
            if (!response.isCommitted()) {
                response.resetBuffer();
                reject(response, 500, "服务内部错误，请联系维护者并提供请求编号。", method);
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

    private static void reject(HttpServletResponse response, int status, String message, String method)
            throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        if (!method.equals("HEAD")) response.getWriter().write(message);
    }
}
