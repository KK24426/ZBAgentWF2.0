/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：限制基础首页访问面，记录脱敏请求诊断并隐藏错误详情。
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 当前只有首页与固定资源。新增业务路由时须同步用户批准的 HTTP 契约及此访问范围。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebRequestFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(WebRequestFilter.class);
    private static final Set<String> PATHS = Set.of("/", "/index.html", "/app.css", "/favicon.svg");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String previous = MDC.get("requestId");
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        long started = System.nanoTime();
        String method = Set.of("GET", "HEAD").contains(request.getMethod()) ? request.getMethod() : "OTHER";
        String path = request.getRequestURI();
        String route = Set.of("/", "/index.html").contains(path) ? "HOME"
                : PATHS.contains(path) ? "ASSET" : "OTHER";
        try {
            response.setHeader("X-Request-ID", requestId);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Content-Security-Policy",
                    "default-src 'none'; style-src 'self'; img-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'");
            response.setHeader("Cache-Control", "no-store");
            if (LogFailureMonitor.hasFailed()) {
                reject(response, 503, "服务暂不可用。", method);
            } else if (method.equals("OTHER")) {
                response.setHeader("Allow", "GET, HEAD");
                reject(response, 405, "不支持的请求方式。", method);
            } else if (!PATHS.contains(path)) {
                reject(response, 404, "页面不存在。", method);
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
        }
    }

    private static void reject(HttpServletResponse response, int status, String message, String method)
            throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        if (!method.equals("HEAD")) response.getWriter().write(message);
    }
}
