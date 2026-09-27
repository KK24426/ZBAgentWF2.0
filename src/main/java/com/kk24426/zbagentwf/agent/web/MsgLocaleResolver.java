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

    /**
     * 验证服务级语言仅为auto或内置三语之一；保存选择，不以当前JVM默认语言替代配置。
     */
    public MsgLocaleResolver(String configured) {
        if (!"auto".equals(configured) && !MsgCatalog.LANGUAGES.contains(configured)) {
            throw new IllegalStateException("zb.msg.locale 必须为 auto、zh-CN、en 或 ja。");
        }
        this.configured = configured;
    }

    /**
     * 优先使用有效语言Cookie，其次服务显式语言，再按Accept-Language协商；把结果缓存在本请求中供过滤器和MVC共用。
     */
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

    /** 按choice的本次请求选择生成Locale，沿用其请求级缓存和回退顺序。 */
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return Locale.forLanguageTag(choice(request).language());
    }

    /**
     * 不支持MVC直接设置语言；偏好由页面Cookie管理，本入口始终抛UnsupportedOperationException。
     */
    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("语言偏好由页面 Cookie 保存。");
    }

    /**
     * 按语言头权重选择支持的基础语言；忽略零权重和通配项，缺失、畸形或不匹配均回落zh-CN，不记录原始请求头。
     */
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

    /**
     * 一次请求的语言选择：language为生效值，automatic为无手动Cookie时的值，preference为页面选项值。
     */
    public record Choice(String language, String automatic, String preference) { }
}
