/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：准备每次运行的独立日志目录和标识，不重复初始化 Logback。
 */
package com.kk24426.zbagentwf.common.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

/** 进程启动前的日志准备；zb.log-dir 优先于 ZB_LOG_DIR，默认相对工作目录 logs。 */
public final class RunLogging {
    private RunLogging() {
    }

    public static Path prepare() throws IOException {
        LogFailureMonitor.reset();
        String root = System.getProperty("zb.log-dir",
                System.getenv().getOrDefault("ZB_LOG_DIR", "logs"));
        String runId = Instant.now().toString().replace(':', '-')
                + "-" + ProcessHandle.current().pid() + "-" + UUID.randomUUID();
        Path directory = Path.of(root).toAbsolutePath().normalize().resolve(runId);
        Files.createDirectories(directory);
        Path log = directory.resolve("application.log");
        try (var output = Files.newOutputStream(log, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            output.flush();
        }
        System.setProperty("zb.run-id", runId);
        System.setProperty("zb.log-file", log.toString());
        System.setProperty("zb.console-charset", System.err.charset().name());
        for (String name : new String[]{"SPRING_DATASOURCE_PASSWORD", "SPRING_DATASOURCE_USERNAME",
                "ZB_TEST_DB_PASSWORD", "ZB_TEST_DB_USERNAME"}) {
            SecretRedactor.register(System.getenv(name));
        }
        SecretRedactor.register(System.getProperty("user.name"));
        return log;
    }
}
