/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证本地化首屏、模板约束和不可执行消息数据的转义。
 */
package com.kk24426.zbagentwf.agent.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.json.JsonMapper;

class HomePageControllerTest {
    @TempDir Path temp;

    @Test
    void existingHomepageUrlsRenderThreeLanguagesIncludingMetadataAndNoScript() throws Exception {
        var messages = MsgCatalog.load(temp);
        var locales = new MsgLocaleResolver("auto");
        var mvc = MockMvcBuilders.standaloneSetup(new HomePageController(messages, locales, template()))
                .setLocaleResolver(locales).addFilters(new WebRequestFilter(messages, locales)).build();
        for (String language : MsgCatalog.LANGUAGES) {
            for (String path : new String[]{"/", "/index.html"}) {
                var response = mvc.perform(get(path).header("Accept-Language", language))
                        .andExpect(status().isOk()).andExpect(content().contentType("text/html;charset=UTF-8"))
                        .andExpect(header().string("Content-Language", language)).andReturn().getResponse();
                String html = response.getContentAsString();
                assertTrue(html.contains("lang=\"" + language + "\""));
                assertTrue(html.contains(messages.get("page.title", java.util.Locale.forLanguageTag(language))));
                assertTrue(html.contains(messages.get("page.chat.noScript", java.util.Locale.forLanguageTag(language))));
                assertFalse(html.contains("{{"));
                assertTrue(html.contains("<script src=\"/chat.js\" defer></script>"));
                assertTrue(response.getHeader("Content-Security-Policy").contains("script-src 'self'"));
            }
        }
        // 资源保护由原过滤器完成，模板和消息文件均没有新增公开路径。
        for (String path : new String[]{"/web/index.html", "/msg/msg_en.properties", "/config/msg.properties"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }

    @Test
    void hostileTranslationsRemainTextInHtmlAttributesAndInertJson() throws Exception {
        String payload = "</template><script>alert('x')</script>\" & <img src=x onerror=alert(1)>";
        Files.writeString(temp.resolve("msg_en.properties"),
                "page.title=" + payload + "\npage.chat.placeholder=" + payload + "\nchat.sending=" + payload + "\n");
        var messages = MsgCatalog.load(temp);
        var locales = new MsgLocaleResolver("en");
        String html = new HomePageController(messages, locales, template())
                .home(new MockHttpServletRequest()).getBody();
        assertNotNull(html);
        assertFalse(html.contains(payload));
        assertFalse(html.contains("<script>alert"));
        assertFalse(html.contains("<img src=x"));
        assertEquals(1, html.split("</template>", -1).length - 1);
        String encoded = html.substring(html.indexOf("<template id=\"msg-catalog\">") + "<template id=\"msg-catalog\">".length(),
                html.indexOf("</template>"));
        var json = JsonMapper.builder().build().readTree(HtmlUtils.htmlUnescape(encoded));
        assertEquals(payload, json.path("messages").path("en").path("chat.sending").asString());
        assertEquals("en", json.path("language").asString());
        assertFalse(json.path("messages").path("en").has("http.internalError"));
    }

    @Test
    void missingOrMalformedTemplateKeysFailAtInitialization() throws Exception {
        var messages = MsgCatalog.load(temp);
        var locales = new MsgLocaleResolver("auto");
        for (String value : new String[]{"{{missing.key}}", "{{broken-key}}"}) {
            assertThrows(IllegalStateException.class, () -> new HomePageController(messages, locales, value));
        }
    }

    private String template() throws Exception {
        return new ClassPathResource("web/index.html").getContentAsString(StandardCharsets.UTF_8);
    }
}
