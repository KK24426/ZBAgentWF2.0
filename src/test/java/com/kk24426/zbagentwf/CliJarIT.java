/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：以真实子进程验证 Boot JAR、CLI 契约、日志及并发隔离。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliJarIT {
    @TempDir Path temp;
    private final Path jar = Path.of(System.getProperty("cli.jar")).toAbsolutePath();

    @Test
    void commandsAndLogSeparation() throws Exception {
        for (String command : List.of("", "help", "--help", "version", "--version")) {
            Result result = run(Map.of(), command.isEmpty() ? List.of() : List.of(command));
            assertEquals(0, result.code, result.err);
            if (command.contains("version")) {
                assertEquals("ZBAgentWF2.0 0.1.0-SNAPSHOT" + System.lineSeparator(), result.out);
            } else {
                assertTrue(result.out.startsWith("ZBAgentWF2.0 CLI" + System.lineSeparator()));
                assertTrue(result.out.contains("显示帮助"));
            }
            assertFalse(result.out.contains("Spring"));
            assertTrue(result.err.contains("Spring Boot"));
            String log = readLog(result.directory);
            assertTrue(log.contains("应用初始化开始"));
            assertTrue(log.contains("容器初始化完成"));
            assertTrue(log.contains("运行结束 exitCode=0"));
            assertTrue(log.contains("runId="));
            assertFalse(log.contains("HikariPool"));
        }
        for (List<String> arguments : List.of(List.of(""), List.of("UNKNOWN-PRIVATE"),
                List.of("HELP"), List.of("unknown", "extra"), List.of("help", "extra"),
                List.of("--help", "extra"), List.of("version", "extra"), List.of("--version", "extra"))) {
            Result result = run(Map.of(), arguments);
            assertEquals(2, result.code);
            assertEquals("", result.out);
            assertTrue(result.err.contains("命令或参数错误"));
            assertFalse(result.err.contains("UNKNOWN-PRIVATE"));
            assertFalse(readLog(result.directory).contains("UNKNOWN-PRIVATE"));
        }
    }

    @Test
    void mysqlMissingConfigurationFailsAndWritesExceptionChain() throws Exception {
        Result result = run(Map.of("spring.profiles.active", "mysql"), List.of("help"));
        assertEquals(1, result.code);
        assertEquals("", result.out);
        String log = readLog(result.directory);
        assertTrue(log.contains("mysql profile"));
        assertTrue(log.contains("Caused by:"));
        assertTrue(log.contains("MySqlConfiguration"));
        assertTrue(log.contains("exitCode=1"));
    }

    @Test
    void unwritableLogRootFailsWithoutStdout() throws Exception {
        Path file = temp.resolve("not-a-directory");
        Files.writeString(file, "fixture");
        Result result = run(Map.of("zb.log-dir", file.toString()), List.of("help"));
        assertEquals(1, result.code);
        assertEquals("", result.out);
        // 此提示使用进程 stderr 的默认编码，测试统一通过 JVM 参数设为 UTF-8。
        assertTrue(result.err.contains("无法初始化日志"));
    }

    @Test
    void concurrentRunsUseSeparateLogs() throws Exception {
        Path shared = temp.resolve("shared");
        Pending first = start(Map.of("zb.log-dir", shared.toString()), List.of("help"));
        Pending second = start(Map.of("zb.log-dir", shared.toString()), List.of("version"));
        assertEquals(0, finish(first).code);
        assertEquals(0, finish(second).code);
        try (var directories = Files.list(shared)) {
            var runs = directories.toList();
            assertEquals(2, runs.size());
            for (Path run : runs) assertTrue(Files.readString(run.resolve("application.log")).contains("exitCode=0"));
        }
    }

    @Test
    void defaultStderrEncodingKeepsLogsAndErrorsReadable() throws Exception {
        Result result = finish(start(Map.of(), List.of("unknown"), false));
        assertEquals(2, result.code);
        assertEquals("", result.out);
        assertTrue(result.err.contains("应用初始化开始"));
        assertTrue(result.err.contains("命令或参数错误：未知命令。"));
        assertTrue(result.err.contains("运行结束"));
        // 文件不跟随控制台编码，仍始终为 UTF-8。
        assertTrue(readLog(result.directory).contains("应用初始化开始"));
    }

    @Test
    void executableJarContainsDependenciesButNoFixtures() throws Exception {
        try (var archive = new JarFile(jar.toFile())) {
            var attributes = archive.getManifest().getMainAttributes();
            assertEquals("com.kk24426.zbagentwf.ZbAgentWfCli", attributes.getValue("Start-Class"));
            assertEquals("0.1.0-SNAPSHOT", attributes.getValue("Implementation-Version"));
            var names = archive.stream().map(java.util.zip.ZipEntry::getName).toList();
            assertTrue(names.stream().anyMatch(n -> n.startsWith("BOOT-INF/lib/mysql-connector-j")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("BOOT-INF/lib/mybatis-")));
            assertFalse(names.stream().anyMatch(n -> n.contains("Fixture") || n.contains("MySqlIT") || n.contains("junit")));
        }
    }

    private Result run(Map<String, String> properties, List<String> arguments) throws Exception {
        return finish(start(properties, arguments));
    }

    private Pending start(Map<String, String> properties, List<String> arguments) throws Exception {
        return start(properties, arguments, true);
    }

    private Pending start(Map<String, String> properties, List<String> arguments, boolean forceUtf8) throws Exception {
        Path directory = Files.createTempDirectory(temp, "process-");
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString());
        if (forceUtf8) {
            command.add("-Dstdout.encoding=UTF-8");
            command.add("-Dstderr.encoding=UTF-8");
        }
        properties.forEach((key, value) -> command.add("-D" + key + "=" + value));
        command.add("-jar");
        command.add(jar.toString());
        command.addAll(arguments);
        var builder = new ProcessBuilder(command).directory(directory.toFile());
        builder.environment().keySet().removeIf(k -> k.startsWith("SPRING_") || k.startsWith("ZB_")
                || Set.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS").contains(k));
        builder.redirectOutput(directory.resolve("stdout.txt").toFile());
        builder.redirectError(directory.resolve("stderr.txt").toFile());
        Charset charset = forceUtf8 ? StandardCharsets.UTF_8 : Charset.forName(System.getProperty("native.encoding"));
        return new Pending(builder.start(), directory, charset);
    }

    private Result finish(Pending pending) throws Exception {
        boolean done = pending.process.waitFor(40, TimeUnit.SECONDS);
        if (!done) {
            pending.process.destroyForcibly();
            pending.process.waitFor();
        }
        assertTrue(done, "CLI 子进程超时");
        return new Result(pending.process.exitValue(),
                Files.readString(pending.directory.resolve("stdout.txt"), pending.charset),
                Files.readString(pending.directory.resolve("stderr.txt"), pending.charset), pending.directory);
    }

    private String readLog(Path directory) throws Exception {
        try (var files = Files.walk(directory.resolve("logs"))) {
            Path file = files.filter(p -> p.getFileName().toString().equals("application.log")).findFirst().orElseThrow();
            return Files.readString(file, StandardCharsets.UTF_8);
        }
    }

    private record Pending(Process process, Path directory, Charset charset) {}
    private record Result(int code, String out, String err, Path directory) {}
}
