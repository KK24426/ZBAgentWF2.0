/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证容器默认无数据库、按接口注入和关闭资源。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Qualifier;
import com.kk24426.zbagentwf.user.InjectionFixture;

class ContextTest {
    @Test
    void rootScanInjectsAcrossPackagesAndDoesNotCreateDataSource() {
        try (var context = new AnnotationConfigApplicationContext(ZbAgentWfApplication.class)) {
            assertTrue(context.getBeansOfType(DataSource.class).isEmpty());
            assertNotNull(context.getBean(com.kk24426.zbagentwf.agent.web.WebRequestFilter.class));
            assertEquals("injected", context.getBean(InjectionFixture.class).value());
        }
    }

    @Test
    void missingAndAmbiguousImplementationsFailClearly() {
        assertThrows(UnsatisfiedDependencyException.class,
                () -> new AnnotationConfigApplicationContext(InjectionFixture.class));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("first", InjectionFixture.Port.class, () -> () -> "first");
            context.registerBean("second", InjectionFixture.Port.class, () -> () -> "second");
            context.refresh();
            assertThrows(NoUniqueBeanDefinitionException.class, () -> context.getBean(InjectionFixture.Port.class));
        }
    }

    @Test
    void qualifierSelectsAndCloseReleasesResources() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("first", InjectionFixture.Port.class, () -> () -> "first");
            context.registerBean("second", InjectionFixture.Port.class, () -> () -> "second");
            context.register(QualifiedConsumer.class);
            context.refresh();
            assertEquals("second", context.getBean(QualifiedConsumer.class).port.get());
        }
        var closed = new java.util.concurrent.atomic.AtomicBoolean();
        var context = new AnnotationConfigApplicationContext();
        context.registerBean("resource", Resource.class, () -> new Resource(closed),
                definition -> definition.setDestroyMethodName("close"));
        context.refresh();
        context.close();
        assertTrue(closed.get());
    }

    static class QualifiedConsumer {
        final InjectionFixture.Port port;
        QualifiedConsumer(@Qualifier("second") InjectionFixture.Port port) { this.port = port; }
    }

    static class Resource implements AutoCloseable {
        final java.util.concurrent.atomic.AtomicBoolean closed;
        Resource(java.util.concurrent.atomic.AtomicBoolean closed) { this.closed = closed; }
        public void close() { closed.set(true); }
    }
}
