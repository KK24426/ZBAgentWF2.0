/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证默认语言及显式配置入口。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class MsgConfigurationTest {
    @Test
    void autoIsDefaultAndExplicitLanguageIsValidated() {
        var configuration = new MsgConfiguration();
        var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "ja");
        assertEquals("ja", configuration.localeResolver(new MockEnvironment()).choice(request).language());
        assertEquals("en", configuration.localeResolver(new MockEnvironment().withProperty("zb.msg.locale", "en"))
                .choice(new MockHttpServletRequest()).language());
        assertThrows(IllegalStateException.class,
                () -> configuration.localeResolver(new MockEnvironment().withProperty("zb.msg.locale", "private-config-value")));
    }
}
