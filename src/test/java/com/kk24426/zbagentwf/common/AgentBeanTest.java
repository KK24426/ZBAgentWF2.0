/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证 AgentBean 默认值、独立属性与不转换输入的访问契约。
 */
package com.kk24426.zbagentwf.common;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AgentBeanTest {
    @Test
    void noArgConstructorLeavesAllPropertiesNull() {
        var bean = new AgentBean();
        assertNull(bean.getBrand());
        assertNull(bean.getName());
        assertNull(bean.getVer());
    }

    @Test
    void propertiesAreIndependent() {
        var bean = new AgentBean();
        bean.setBrand("提供方");
        assertNull(bean.getName());
        assertNull(bean.getVer());
        bean.setName("模型");
        assertEquals("提供方", bean.getBrand());
        assertNull(bean.getVer());
        bean.setVer("版本一");
        assertEquals("提供方", bean.getBrand());
        assertEquals("模型", bean.getName());
        assertEquals("版本一", bean.getVer());
        bean.setBrand(null);
        assertEquals("模型", bean.getName());
        assertEquals("版本一", bean.getVer());
        bean.setName("");
        assertNull(bean.getBrand());
        assertEquals("版本一", bean.getVer());
    }

    @Test
    void chineseEmptyWhitespaceAndNullValuesRoundTripUnchanged() {
        var bean = new AgentBean();
        for (String value : new String[]{"中文模型🙂", "", "  模型\t ", null}) {
            bean.setBrand(value);
            bean.setName(value);
            bean.setVer(value);
            assertEquals(value, bean.getBrand());
            assertEquals(value, bean.getName());
            assertEquals(value, bean.getVer());
        }
    }
}
