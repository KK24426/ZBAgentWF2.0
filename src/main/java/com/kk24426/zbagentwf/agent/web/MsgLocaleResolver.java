/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按浏览器偏好、服务配置和语言请求头解析请求级语言。
 */
package com.kk24426.zbagentwf.agent.web;

import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.web.servlet.LocaleResolver;

/** 过滤器与 MVC 共用 request 属性缓存，避免并发请求之间的语言串扰。 */
public final class MsgLocaleResolver implements LocaleResolver {
    public static final String COOKIE_NAME = "zb.locale";
    private static final String ATTRIBUTE = MsgLocaleResolver.class.getName() + ".choice";
    private final String configured;

    public MsgLocaleResolver(String configured) {
        if (!"auto".equals(configured) && !MsgCatalog.LANGUAGES.contains(configured)) {
            throw new IllegalStateException("zb.msg.locale 必须为 auto、zh-CN、en 或 ja。");
        }
        this.configured = configured;
    }

    public Choice choice(HttpServletRequest request) {
        if (request.getAttribute(ATTRIBUTE) instanceof Choice existing) return existing;
        String automatic = "auto".equals(configured) ? browserLanguage(request.getHeader("Accept-Language")) : configured;
        String preference = "auto";
        var cookies = request.getCookies();
        if (cookies != null) {
            for (var cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName()) && MsgCatalog.LANGUAGES.contains(cookie.getValue())) {
                    preference = cookie.getValue();
                    break;
                }
            }
        }
        var choice = new Choice("auto".equals(preference) ? automatic : preference, automatic, preference);
        request.setAttribute(ATTRIBUTE, choice);
        return choice;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return Locale.forLanguageTag(choice(request).language());
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("语言偏好由页面 Cookie 保存。");
    }

    private static String browserLanguage(String header) {
        if (header == null || header.isBlank()) return "zh-CN";
        try {
            // parse 按权重稳定排序；忽略无具体语言的通配符，不读取或记录原始请求值。
            for (var range : Locale.LanguageRange.parse(header)) {
                if (range.getWeight() == 0 || range.getRange().contains("*")) continue;
                String primary = range.getRange().split("-", 2)[0];
                if ("zh".equals(primary)) return "zh-CN";
                if ("en".equals(primary) || "ja".equals(primary)) return primary;
            }
        } catch (IllegalArgumentException invalid) {
            // 畸形语言头不是业务错误，不能改变原路由的 404/405/415/503。
        }
        return "zh-CN";
    }

    public record Choice(String language, String automatic, String preference) { }
}
