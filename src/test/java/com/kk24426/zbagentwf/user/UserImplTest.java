/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证空父接口声明及仅用于测试的接口继承、实现关系。
 */
package com.kk24426.zbagentwf.user;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class UserImplTest {
    @Test
    void parentIsAnInterfaceWithoutDeclaredMethods() {
        assertTrue(UserInterface.class.isInterface());
        assertEquals(0, UserInterface.class.getDeclaredMethods().length);
    }

    @Test
    void implementationCanBeUsedThroughChildAndParentInterfaces() {
        ChildPort child = new ChildImplementation();
        UserInterface parent = child;
        assertSame(child, parent);
        assertInstanceOf(UserInterface.class, child);
        assertTrue(UserInterface.class.isAssignableFrom(ChildPort.class));
    }

    // 只验证 Java 类型关系，不注册 Spring Bean，不代表已实现的业务接口。
    private interface ChildPort extends UserInterface {
    }

    private static class ChildImplementation implements ChildPort {
    }
}
