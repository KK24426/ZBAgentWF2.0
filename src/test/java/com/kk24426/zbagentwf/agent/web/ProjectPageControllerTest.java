/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证项目页面三语、安全模板渲染和精确只读路由。
 */
package com.kk24426.zbagentwf.agent.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProjectPageControllerTest {
    @TempDir Path temp;

    @Test
    void projectPageUsesExistingLocaleRulesAndOnlyAllowsItsExactReadRoutes() throws Exception {
        var messages = MsgCatalog.load(temp);
        var locales = new MsgLocaleResolver("auto");
        var mvc = MockMvcBuilders.standaloneSetup(new ProjectPageController(messages, locales, template()))
                .setLocaleResolver(locales).addFilters(new WebRequestFilter(messages, locales)).build();
        for (String language : MsgCatalog.LANGUAGES) {
            for (String path : new String[]{"/projects", "/projects.html"}) {
                String html = mvc.perform(get(path).header("Accept-Language", language))
                        .andExpect(status().isOk()).andExpect(header().string("Content-Language", language))
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andReturn().getResponse().getContentAsString();
                assertTrue(html.contains(messages.get("project.title", Locale.forLanguageTag(language))));
                assertTrue(html.contains(messages.get("project.noScript", Locale.forLanguageTag(language))));
                assertFalse(html.contains("{{"));
            }
        }
        for (String path : new String[]{"/projects", "/projects.html", "/projects.js", "/projects.css", "/messages.js"}) {
            mvc.perform(post(path)).andExpect(status().isMethodNotAllowed());
        }
        for (String path : new String[]{"/projects/", "/projects/private", "/web/projects.html", "/config/agents.properties"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }

    @Test
    void projectTranslationsRemainTextAndInvalidTemplateKeysFailAtStartup() throws Exception {
        String payload = "</template><script>alert(1)</script><img src=x onerror=alert(1)>\"";
        Files.writeString(temp.resolve("msg_en.properties"),
                "project.title=" + payload + "\nproject.goalPlaceholder=" + payload + "\n");
        var messages = MsgCatalog.load(temp);
        var locales = new MsgLocaleResolver("en");
        var controller = new ProjectPageController(messages, locales, template());
        String html = controller.projects(new org.springframework.mock.web.MockHttpServletRequest()).getBody();
        assertNotNull(html);
        assertFalse(html.contains(payload));
        assertFalse(html.contains("<script>alert"));
        assertFalse(html.contains("<img src=x"));
        assertEquals(1, html.split("</template>", -1).length - 1);
        assertThrows(IllegalStateException.class,
                () -> new ProjectPageController(messages, locales, "{{project.missing}}"));
    }

    private String template() throws Exception {
        return new ClassPathResource("web/projects.html").getContentAsString(StandardCharsets.UTF_8);
    }
}
