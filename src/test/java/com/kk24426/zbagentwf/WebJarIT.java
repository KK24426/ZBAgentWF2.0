/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：通过真实 JAR 进程验证常驻 Web、静态页面、失败路径和资源隔离。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebJarIT {
    @TempDir Path temp;
    private final Path jar = Path.of(System.getProperty("web.jar")).toAbsolutePath();

    @Test
    void malformedProtocolNeverLeaksRawInputsBeforeFilter() throws Exception {
        String[] requests = {
                "GET /private-target?value=private-query< HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n",
                "PRIVATE-METHOD< / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n",
                "GET / HTTP/1.1\r\nHost: localhost\r\nprivate-header<: private-value\r\nConnection: close\r\n\r\n"};
        // 每类请求独立进程，确保覆盖容器首次 INFO 解析错误（随后错误可能降为 DEBUG）。
        for (String request : requests) {
            try (Pending server = start(Map.of())) {
                int port = awaitReady(server);
                String response;
                try (Socket socket = new Socket("127.0.0.1", port)) {
                    socket.setSoTimeout(5000);
                    socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                }
                assertTrue(response.startsWith("HTTP/1.1 400"));
                for (String output : List.of(response, server.error(), readLog(server.directory))) {
                    assertFalse(output.toLowerCase(Locale.ROOT).contains("private-"));
                }
                assertTrue(readLog(server.directory).contains("HTTP 容器诊断"));
                assertTrue(readLog(server.directory).contains("IllegalArgumentException"));
                assertTrue(server.process.isAlive());
            }
        }
    }

    @Test
    void servesHomepageAndSafeErrorsWhileRemainingAlive() throws Exception {
        try (Pending server = start(Map.of())) {
            int port = awaitReady(server);
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
                var home = get(client, port, "/");
                assertEquals(200, home.statusCode());
                assertTrue(home.body().contains("三个包，清晰协作"));
                assertTrue(home.body().contains("lang=\"zh-CN\""));
                for (String element : List.of("chat-form", "chat-message", "chat-send", "chat-result")) {
                    assertTrue(home.body().contains("id=\"" + element + "\""));
                }
                assertTrue(home.headers().firstValue("Content-Security-Policy").orElseThrow().contains("frame-ancestors 'none'"));
                assertTrue(home.headers().firstValue("Content-Security-Policy").orElseThrow().contains("script-src 'self'"));
                assertTrue(home.headers().firstValue("Content-Security-Policy").orElseThrow().contains("connect-src 'self'"));
                assertTrue(home.headers().firstValue("X-Request-ID").isPresent());
                assertEquals(200, get(client, port, "/app.css").statusCode());
                assertEquals(200, get(client, port, "/favicon.svg").statusCode());
                var script = get(client, port, "/chat.js");
                assertEquals(200, script.statusCode());
                assertTrue(script.body().contains("/api/chat"));
                var scriptHead = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/chat.js"))
                        .timeout(Duration.ofSeconds(5)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, scriptHead.statusCode());
                assertEquals("", scriptHead.body());
                var notFound = get(client, port, "/unknown-private?token=private-query");
                assertEquals(404, notFound.statusCode());
                assertFalse(notFound.body().contains("unknown-private"));
                assertFalse(notFound.body().contains("Exception"));
                var method = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                        .timeout(Duration.ofSeconds(5)).method("PRIVATE-METHOD", HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(405, method.statusCode());
                var head = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                        .timeout(Duration.ofSeconds(5)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, head.statusCode());
                assertEquals("", head.body());
            }
            assertTrue(server.process.isAlive());
            String log = readLog(server.directory);
            assertTrue(log.contains("Web 服务就绪"));
            assertTrue(log.contains("route=HOME"));
            assertTrue(log.contains("requestId="));
            assertFalse(log.contains("unknown-private"));
            assertFalse(log.contains("private-query"));
            assertFalse(log.contains("PRIVATE-METHOD"));
            assertEquals("", Files.readString(server.directory.resolve("stdout.txt")));
        }
    }

    @Test
    void productionChatIsUnavailableAndInvalidRequestsStayPrivate() throws Exception {
        try (Pending server = start(Map.of())) {
            int port = awaitReady(server);
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
                var unavailable = chat(client, port, "application/json", "{\"message\":\"普通隐私输入\"}");
                assertEquals(503, unavailable.statusCode());
                assertEquals("Agent 尚未接入，暂时无法生成回复。", unavailable.body());
                assertTrue(unavailable.headers().firstValue("X-Request-ID").isPresent());
                for (String body : List.of("", "null", "{}", "{\"message\":null}", "{\"message\":123}",
                        "{\"message\":true}", "{\"message\":\" \\n\"}", "{\"message\":普通隐私标记}",
                        "{\"message\":\"" + "a".repeat(4001) + "\"}")) {
                    var invalid = chat(client, port, "application/json", body);
                    assertEquals(400, invalid.statusCode());
                    assertEquals("请求无效：message 必须是非空白字符串，且长度不能超过 4000。", invalid.body());
                }
                var media = chat(client, port, "text/plain", "普通隐私输入");
                assertEquals(415, media.statusCode());
                assertEquals("仅支持 JSON 请求。", media.body());
                var method = get(client, port, "/api/chat");
                assertEquals(405, method.statusCode());
                assertEquals("POST", method.headers().firstValue("Allow").orElseThrow());
                var head = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                        .timeout(Duration.ofSeconds(5)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(405, head.statusCode());
                assertEquals("", head.body());
            }
            for (String output : List.of(server.error(), readLog(server.directory))) {
                assertFalse(output.contains("普通隐私"));
                assertTrue(output.contains("AgentUnavailableException"));
                assertTrue(output.contains("HttpMessageNotReadableException"));
                assertTrue(output.contains("ChatController.java"));
                assertTrue(output.contains("Caused by:"));
            }
            assertTrue(readLog(server.directory).contains("method=POST route=CHAT"));
            assertTrue(server.process.isAlive());
        }
    }

    @Test
    void missingMysqlConfigFailsWithExceptionChain() throws Exception {
        try (Pending server = start(Map.of("spring.profiles.active", "mysql"))) {
            assertExit(server, 1);
            String log = readLog(server.directory);
            assertTrue(log.contains("mysql profile"));
            assertTrue(log.contains("Caused by:"));
            assertTrue(log.contains("Web 服务启动失败"));
        }
    }

    @Test
    void rejectsLegacyCliWithoutEchoingInput() throws Exception {
        try (Pending server = start(Map.of(), "help", "private-argument")) {
            assertExit(server, 2);
            assertTrue(server.error().contains("不接受命令参数"));
            assertFalse(server.error().contains("private-argument"));
            assertEquals("", Files.readString(server.directory.resolve("stdout.txt")));
        }
    }

    @Test
    void unwritableLogDirectoryFails() throws Exception {
        Path file = temp.resolve("not-a-directory");
        Files.writeString(file, "fixture");
        try (Pending server = start(Map.of("zb.log-dir", file.toString()))) {
            assertExit(server, 1);
            assertTrue(server.error().contains("无法初始化日志"));
        }
    }

    @Test
    void occupiedPortFailsInsteadOfStartingAnotherService() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             Pending server = start(Map.of("server.port", Integer.toString(occupied.getLocalPort())))) {
            assertExit(server, 1);
            assertTrue(readLog(server.directory).contains("Web 服务启动失败"));
        }
    }

    @Test
    void concurrentServersHaveIndependentLogsAndPorts() throws Exception {
        Path logs = temp.resolve("shared-logs");
        try (Pending first = start(Map.of("zb.log-dir", logs.toString()));
             Pending second = start(Map.of("zb.log-dir", logs.toString()))) {
            assertNotEquals(awaitReady(first), awaitReady(second));
            try (var paths = Files.list(logs)) {
                assertEquals(2, paths.count());
            }
        }
    }

    @Test
    void executableJarContainsWebButNoCliOrFixtures() throws Exception {
        try (var archive = new JarFile(jar.toFile())) {
            assertEquals("com.kk24426.zbagentwf.ZbAgentWfApplication",
                    archive.getManifest().getMainAttributes().getValue("Start-Class"));
            var names = archive.stream().map(java.util.zip.ZipEntry::getName).toList();
            assertTrue(names.contains("BOOT-INF/classes/static/index.html"));
            assertTrue(names.contains("BOOT-INF/classes/static/chat.js"));
            assertTrue(names.contains("BOOT-INF/classes/com/kk24426/zbagentwf/agent/chat/AgentChatImpl.class"));
            assertTrue(names.contains("BOOT-INF/classes/com/kk24426/zbagentwf/agent/codex/CodexAgentExec.class"));
            assertTrue(names.contains("BOOT-INF/classes/com/kk24426/zbagentwf/agent/project/ProjectUserifImpl.class"));
            assertTrue(names.contains("BOOT-INF/classes/com/kk24426/zbagentwf/user/agent/userif/AgentExecutor.class"));
            assertTrue(names.contains("BOOT-INF/classes/com/kk24426/zbagentwf/agent/registry/AgentExecFactoryImpl.class"));
            assertFalse(names.contains("BOOT-INF/classes/com/kk24426/zbagentwf/user/agent/userif/AgentExec.class"));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("BOOT-INF/lib/tomcat-embed-core")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("BOOT-INF/lib/mysql-connector-j")));
            assertFalse(names.stream().anyMatch(n -> n.contains("CliApplication") || n.contains("ZbAgentWfCli")
                    || n.contains("Fixture") || n.contains("MySqlIT") || n.contains("junit")
                    || n.contains("AgentBeanTest") || n.contains("UserInterfaceTest")
                    || n.contains("ProjectModelTest") || n.contains("AgentExecContractTest")
                    || n.contains("ProjectConfigurationTest")
                    || n.contains("AgentConfigurationTest") || n.contains("ExecutionResourcesTest") || n.contains("AgentRegistryTest")
                    || n.contains("CodexAgentExecTest") || n.contains("CodexRequirementPlannerTest") || n.contains("ProjectUserifImplTest")
                    || n.contains("ChatAgentFixture") || n.contains("ChatControllerTest") || n.contains("ChatServiceTest")));
        }
    }

    @Test
    void projectConfigurationFileLoadsWithoutCreatingItsRelativeDirectory() throws Exception {
        String relative = "项目配置/child/../root";
        try (Pending server = startConfigured(Map.of(), Map.of(), relative)) {
            awaitReady(server);
            assertFalse(Files.exists(server.directory.resolve("项目配置")));
            assertEquals("", Files.readString(server.directory.resolve("stdout.txt")));
        }
    }

    @Test
    void missingBlankMalformedAndNonDirectoryProjectRootsFailStartup() throws Exception {
        Path file = Files.writeString(temp.resolve("root-is-a-file"), "fixture");
        for (String root : new String[]{null, "", " ", "private-config-value" + (char) 0, file.toString()}) {
            try (Pending server = startConfigured(Map.of(), Map.of(), root)) {
                assertExit(server, 1);
                String log = readLog(server.directory);
                assertTrue(log.contains("zb.project.root"));
                assertTrue(log.contains("Web 服务启动失败"));
                assertFalse(log.contains("Web 服务就绪"));
                assertFalse(log.contains("private-config-value"));
                assertFalse(server.error().contains("private-config-value"));
                assertEquals("", Files.readString(server.directory.resolve("stdout.txt")));
            }
        }
    }

    @Test
    void environmentOverridesFileAndJvmOverridesEnvironment() throws Exception {
        // 环境变量能单独提供必填配置，也能覆盖一个无效的文件值。
        Path invalid = Files.writeString(temp.resolve("invalid-file-root"), "fixture");
        try (Pending server = startConfigured(Map.of(), Map.of("ZB_PROJECT_ROOT", "env-projects"), null)) {
            awaitReady(server);
            assertFalse(Files.exists(server.directory.resolve("env-projects")));
        }
        try (Pending server = startConfigured(Map.of(), Map.of("ZB_PROJECT_ROOT", "env-projects"), invalid.toString())) {
            awaitReady(server);
        }
        // 空环境变量不能偷偷回退到合法文件值。
        try (Pending server = startConfigured(Map.of(), Map.of("ZB_PROJECT_ROOT", ""), "file-projects")) {
            assertExit(server, 1);
            assertTrue(readLog(server.directory).contains("必须显式配置非空的 zb.project.root"));
        }
        try (Pending server = startConfigured(Map.of("zb.project.root", "jvm-projects"),
                Map.of("ZB_PROJECT_ROOT", ""), invalid.toString())) {
            awaitReady(server);
            assertFalse(Files.exists(server.directory.resolve("jvm-projects")));
        }
        // JVM 中的空值优先，不能回退到合法环境变量或文件值。
        try (Pending server = startConfigured(Map.of("zb.project.root", ""),
                Map.of("ZB_PROJECT_ROOT", "env-projects"), "file-projects")) {
            assertExit(server, 1);
        }
    }

    private HttpResponse<String> chat(HttpClient client, int port, String contentType, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                        .timeout(Duration.ofSeconds(5)).header("Content-Type", contentType)
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(HttpClient client, int port, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private Pending start(Map<String, String> properties, String... arguments) throws Exception {
        return startConfigured(properties, Map.of(), "projects", arguments);
    }

    private Pending startConfigured(Map<String, String> properties, Map<String, String> environment,
                                    String fileRoot, String... arguments) throws Exception {
        Path directory = Files.createTempDirectory(temp, "web-");
        if (fileRoot != null) {
            Path config = Files.createDirectory(directory.resolve("config")).resolve("project.properties");
            var values = new Properties();
            values.setProperty("zb.project.root", fileRoot);
            try (var output = Files.newOutputStream(config)) {
                values.store(output, "isolated project configuration");
            }
        }
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString());
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.add("-Dserver.port=0");
        properties.forEach((key, value) -> command.add("-D" + key + "=" + value));
        command.add("-jar");
        command.add(jar.toString());
        command.addAll(List.of(arguments));
        var builder = new ProcessBuilder(command).directory(directory.toFile());
        builder.environment().keySet().removeIf(k -> k.startsWith("SPRING_") || k.startsWith("ZB_")
                || k.startsWith("SERVER_") || k.startsWith("LOGGING_")
                || Set.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS").contains(k));
        builder.environment().putAll(environment);
        builder.redirectOutput(directory.resolve("stdout.txt").toFile());
        builder.redirectError(directory.resolve("stderr.txt").toFile());
        return new Pending(builder.start(), directory);
    }

    private int awaitReady(Pending server) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        var pattern = Pattern.compile("Web 服务就绪 port=(\\d+)");
        while (System.nanoTime() < deadline && server.process.isAlive()) {
            var match = pattern.matcher(server.error());
            if (match.find()) return Integer.parseInt(match.group(1));
            Thread.sleep(100);
        }
        fail("Web 服务未就绪：" + server.error());
        return -1;
    }

    private void assertExit(Pending server, int code) throws Exception {
        assertTrue(server.process.waitFor(45, TimeUnit.SECONDS), "启动失败进程未退出");
        assertEquals(code, server.process.exitValue(), server.error());
    }

    private String readLog(Path directory) throws Exception {
        try (var files = Files.walk(directory.resolve("logs"))) {
            return Files.readString(files.filter(p -> p.getFileName().toString().equals("application.log"))
                    .findFirst().orElseThrow(), StandardCharsets.UTF_8);
        }
    }

    private record Pending(Process process, Path directory) implements AutoCloseable {
        String error() throws Exception { return Files.readString(directory.resolve("stderr.txt"), StandardCharsets.UTF_8); }
        public void close() throws Exception {
            process.destroy();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                assertTrue(process.waitFor(5, TimeUnit.SECONDS), "未能回收测试进程");
            }
        }
    }
}
