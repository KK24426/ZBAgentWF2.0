/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：为执行和项目测试提供隔离的 Java 进程启动参数。
 */
package com.kk24426.zbagentwf.agent.codex;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** 为多个直接调用者创建同一协议的Java替身客户端和规则目录，不调用真实模型。 */
public final class CodexFixtureSupport {
    private CodexFixtureSupport() { }

    /** 默认十五秒调用预算，场景只控制测试替身行为。 */
    public static CodexClient client(String scenario) { return client(scenario, Duration.ofSeconds(15)); }

    /** 自定义故障超时，模型参数含shell字符以验证参数不会被解释执行。 */
    public static CodexClient client(String scenario, Duration timeout) {
        return client(scenario, timeout, "fixture-model ; $(literal)");
    }

    /** 使用结构化Java启动前缀并初始化测试安全规则；构造本身不启动子进程。 */
    public static CodexClient client(String scenario, Duration timeout, String model) {
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        var client = new CodexClient(List.of(java, "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-cp", classpath,
                FakeCodexFixture.class.getName(), scenario), model, timeout);
        client.initializeSecurity(new com.kk24426.zbagentwf.common.agent.model.Prompt("fixture-security-规则"));
        return client;
    }
    /** 在测试隔离目录显式写入五类规则并加载，不向正式配置写入。 */
    public static com.kk24426.zbagentwf.agent.prompt.PromptCatalog prompts(Path base) {
        try {
            Path directory = java.nio.file.Files.createDirectories(base.resolve("prompt-fixture"));
            for (String name : List.of("default", "security", "planning", "development", "review")) {
                java.nio.file.Files.writeString(directory.resolve(name + ".txt"), "fixture-" + name + "-规则");
            }
            return com.kk24426.zbagentwf.agent.prompt.PromptCatalog.load(directory);
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
}
