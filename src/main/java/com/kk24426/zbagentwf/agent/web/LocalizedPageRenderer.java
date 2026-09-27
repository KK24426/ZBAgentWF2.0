/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：为首页和项目页校验固定消息占位符并安全渲染三语HTML。
 */
package com.kk24426.zbagentwf.agent.web;

import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.json.JsonMapper;

/** 模板不是静态公开资源；只允许固定消息 key 和两个内部占位符，不执行模板表达式。 */
final class LocalizedPageRenderer {
    private static final Pattern SLOT = Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)}}");
    private final MsgCatalog messages;
    private final MsgLocaleResolver locales;
    private final String template;
    private final JsonMapper json = JsonMapper.builder().build();

    LocalizedPageRenderer(MsgCatalog messages, MsgLocaleResolver locales,
                              String template) {
        this.messages = messages;
        this.locales = locales;
        this.template = template;
        var slots = SLOT.matcher(template);
        while (slots.find()) {
            String key = slots.group(1);
            if (!key.equals("_locale") && !key.equals("_catalog") && !messages.contains(key)) {
                throw new IllegalStateException("页面模板引用了未知 msg key。");
            }
        }
        String remainder = slots.replaceAll("");
        if (remainder.contains("{{") || remainder.contains("}}")) {
            throw new IllegalStateException("页面消息占位符格式非法。");
        }
    }

    ResponseEntity<String> render(HttpServletRequest request) {
        var choice = locales.choice(request);
        Locale locale = locales.resolveLocale(request);
        String bootstrap = json.writeValueAsString(Map.of(
                "language", choice.language(), "automatic", choice.automatic(), "preference", choice.preference(),
                "messages", messages.browserMessages()));
        var output = new StringBuilder();
        var slots = SLOT.matcher(template);
        while (slots.find()) {
            String value = switch (slots.group(1)) {
                case "_locale" -> choice.language();
                case "_catalog" -> bootstrap;
                default -> messages.get(slots.group(1), locale);
            };
            // JSON 也必须做 HTML 文本转义，防止译文中的 </template> 提前闭合元素。
            slots.appendReplacement(output, Matcher.quoteReplacement(HtmlUtils.htmlEscape(value, "UTF-8")));
        }
        slots.appendTail(output);
        return ResponseEntity.ok().header("Content-Language", choice.language())
                .header("Vary", "Cookie, Accept-Language").body(output.toString());
    }
}
