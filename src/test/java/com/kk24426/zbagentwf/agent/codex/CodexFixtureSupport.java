/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：为执行和项目测试提供隔离的 Java 进程启动参数。
 */
package com.kk24426.zbagentwf.agent.codex;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public final class CodexFixtureSupport {
    private CodexFixtureSupport() { }

    public static CodexClient client(String scenario) { return client(scenario, Duration.ofSeconds(15)); }

    public static CodexClient client(String scenario, Duration timeout) {
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return new CodexClient(List.of(java, "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-cp", classpath,
                FakeCodexFixture.class.getName(), scenario), "fixture-model ; $(literal)", timeout);
    }
}
