/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证语言选择优先级、请求头容错与并发请求隔离。
 */
package com.kk24426.zbagentwf.agent.web;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.servlet.http.Cookie;
import java.util.Locale;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class MsgLocaleResolverTest {
    @Test
    void manualCookieWinsThenConfigurationThenBrowserThenChinese() {
        var request = request("ja-JP,en;q=0.5");
        var configured = new MsgLocaleResolver("en");
        assertEquals("en", configured.choice(request).language());
        request = request("ja");
        request.setCookies(new Cookie("zb.locale", "zh-CN"));
        var choice = configured.choice(request);
        assertEquals("zh-CN", choice.language());
        assertEquals("en", choice.automatic());
        assertEquals("zh-CN", choice.preference());
        assertEquals("ja", new MsgLocaleResolver("auto").choice(request("ja-JP")).language());
        assertEquals("zh-CN", new MsgLocaleResolver("auto").choice(request("fr-FR")).language());
        request = request("ja");
        request.setCookies(new Cookie("zb.locale", "unsupported"));
        assertEquals("ja", new MsgLocaleResolver("auto").choice(request).language());
    }

    @Test
    void weightsRegionsEqualWeightsAndWildcardsHaveDeterministicResults() {
        var resolver = new MsgLocaleResolver("auto");
        String[][] examples = {
                {"en;q=0.3,ja-JP;q=0.9", "ja"}, {"ja;q=0.5,en;q=0.5", "ja"},
                {"en;q=0.5,ja;q=0.5", "en"}, {"ja;q=0,en-US;q=0.2", "en"},
                {"*;q=1,en-GB;q=0.1", "en"}, {"zh-TW", "zh-CN"}, {"EN-us", "en"},
                {"de,*", "zh-CN"}, {"en;q=0,ja;q=0", "zh-CN"},
                {"ja;q=broken,en;q=0.5", "zh-CN"}, {"en;q=2", "zh-CN"}, {"", "zh-CN"}
        };
        for (var example : examples) assertEquals(example[1], resolver.choice(request(example[0])).language());
        assertEquals(Locale.SIMPLIFIED_CHINESE, resolver.resolveLocale(new MockHttpServletRequest()));
    }

    @Test
    void resultIsCachedOnlyOnItsRequestAndConcurrentChoicesNeverLeak() throws Exception {
        var resolver = new MsgLocaleResolver("auto");
        var first = request("en");
        assertEquals("en", resolver.choice(first).language());
        first.removeHeader("Accept-Language");
        first.addHeader("Accept-Language", "ja");
        assertEquals("en", resolver.choice(first).language());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var english = executor.submit(() -> resolver.resolveLocale(request("en")));
            var japanese = executor.submit(() -> resolver.resolveLocale(request("ja")));
            assertEquals(Locale.ENGLISH, english.get());
            assertEquals(Locale.JAPANESE, japanese.get());
        }
    }

    @Test
    void invalidExplicitConfigurationFailsWithoutEchoingItsValue() {
        for (String value : new String[]{"", " ", "fr", "private-config-value"}) {
            var failure = assertThrows(IllegalStateException.class, () -> new MsgLocaleResolver(value));
            assertFalse(failure.getMessage().contains("private-config-value"));
        }
    }

    private MockHttpServletRequest request(String header) {
        var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", header);
        return request;
    }
}
