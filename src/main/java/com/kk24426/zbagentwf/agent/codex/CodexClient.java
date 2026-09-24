/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：使用结构化命令、标准输入和 JSONL 调用本机 Codex，并管理进程资源。
 */
package com.kk24426.zbagentwf.agent.codex;

import com.kk24426.zbagentwf.common.logging.SecretRedactor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import tools.jackson.databind.JsonNode;

/**
 * Codex 专用技术适配器。路径、模型选择参数和超时由组合调用方明确传入；构造不启动进程。
 * 不读取或推断 AgentBean 字段，不更改本机 CLI 的配置、认证或规则。
 */
public final class CodexClient {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CodexClient.class);
    private static final int OUTPUT_LIMIT = 8 * 1024 * 1024;
    private final List<String> executable;
    private final String model;
    private final Duration timeout;

    public CodexClient(Path executable, String model, Duration timeout) {
        this(List.of(executable.toAbsolutePath().normalize().toString()), model, timeout);
    }

    // 仅同包进程测试替换启动前缀；正式调用只有独立可执行文件，不接受 shell 字符串。
    CodexClient(List<String> executable, String model, Duration timeout) {
        if (executable == null || executable.isEmpty() || model == null || model.isBlank()
                || timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("必须提供 Codex 可执行文件、模型选择参数和正值超时。");
        }
        timeout.toNanos();
        this.executable = List.copyOf(executable);
        this.model = model;
        this.timeout = timeout;
    }

    Response run(Path directory, String prompt, String schema, boolean readOnly, Consumer<String> diagnostics) {
        Process process = null;
        Path schemaFile = null;
        ExecutorService pipes = Executors.newVirtualThreadPerTaskExecutor();
        Set<ProcessHandle> descendants = new LinkedHashSet<>();
        DiagnosticBuffer stderr = new DiagnosticBuffer();
        String callId = java.util.UUID.randomUUID().toString();
        long start = System.nanoTime();
        try {
            LOG.debug("Codex 进程准备 callId={} readOnly={}", callId, readOnly);
            schemaFile = Files.createTempFile("zbagentwf-codex-schema-", ".json");
            Files.writeString(schemaFile, schema, StandardCharsets.UTF_8);
            var command = new ArrayList<>(executable);
            command.addAll(List.of("-a", "never", "exec", "--json", "--ephemeral", "--color", "never",
                    "--skip-git-repo-check", "--sandbox", readOnly ? "read-only" : "workspace-write",
                    "--model", model, "--output-schema", schemaFile.toString(), "-"));
            process = new ProcessBuilder(command).directory(directory.toFile()).start();
            Process running = process;
            Future<byte[]> output = pipes.submit(() -> readOutput(running.getInputStream()));
            Future<?> errors = pipes.submit(() -> { stderr.read(running.getErrorStream()); return null; });
            Future<?> input = pipes.submit(() -> {
                try (var stream = running.getOutputStream()) {
                    stream.write(prompt.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });
            while (true) {
                running.descendants().forEach(descendants::add);
                checkCompleted(output);
                checkCompleted(errors);
                long remaining = remaining(start);
                if (remaining <= 0) throw new TimeoutException();
                if (running.waitFor(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(50)), TimeUnit.NANOSECONDS)) break;
            }
            int exit = running.exitValue();
            LOG.debug("Codex 进程退出 callId={} exitCode={}", callId, exit);
            // 主进程退出不代表继承管道的后代已经退出，收尾仍有固定上限。
            long drainDeadline = System.nanoTime() + Math.min(Math.max(1, remaining(start)), TimeUnit.SECONDS.toNanos(2));
            byte[] bytes = output.get(Math.max(1, drainDeadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            errors.get(Math.max(1, drainDeadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            // CLI 可能鉴权失败后不读取 stdin；先保留完整的受限 stderr，再报告退出码。
            if (exit != 0) throw new IllegalStateException("Codex 进程失败，退出码=" + exit + "。");
            input.get(Math.max(1, drainDeadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            return decode(new String(bytes, StandardCharsets.UTF_8));
        } catch (InterruptedException failure) {
            LOG.debug("Codex 进程中断 callId={}", callId);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Codex 执行被中断。", failure);
        } catch (TimeoutException failure) {
            LOG.debug("Codex 进程超时 callId={}", callId);
            throw new IllegalStateException("Codex 执行或进程收尾超时。", failure);
        } catch (IOException | ExecutionException failure) {
            throw new IllegalStateException("Codex 进程通信失败。", failure);
        } finally {
            if (process != null) {
                process.descendants().forEach(descendants::add);
                descendants.forEach(handle -> { if (handle.isAlive()) handle.destroyForcibly(); });
                if (process.isAlive()) process.destroyForcibly();
                close(process.getOutputStream());
                close(process.getInputStream());
                close(process.getErrorStream());
            }
            pipes.shutdownNow();
            // 不使用 ExecutorService.close()，避免故障管道无限延长已超时的调用。
            boolean interrupted = Thread.interrupted();
            try {
                if (process != null) process.waitFor(2, TimeUnit.SECONDS);
                pipes.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException failure) {
                interrupted = true;
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
            if (schemaFile != null) {
                try { Files.deleteIfExists(schemaFile); }
                catch (IOException failure) {
                    stderr.appendFixed("\n[临时 schema 清理失败]\n");
                    LOG.warn("Codex 临时 schema 清理失败", failure);
                }
            }
            diagnostics.accept(stderr.safeText());
        }
    }

    private long remaining(long start) { return timeout.toNanos() - (System.nanoTime() - start); }

    private static void checkCompleted(Future<?> future) throws ExecutionException, InterruptedException {
        if (future.isDone()) future.get();
    }

    private static byte[] readOutput(InputStream stream) throws IOException {
        try (stream; var bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (bytes.size() + count > OUTPUT_LIMIT) throw new IOException("Codex 输出超过处理上限。");
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        }
    }

    private static void close(java.io.Closeable stream) {
        try { stream.close(); }
        catch (IOException failure) {
            LOG.warn("Codex 管道关闭失败", failure);
        }
    }

    private static Response decode(String lines) {
        JsonNode lastMessage = null;
        Long tokens = null;
        boolean completed = false;
        for (String line : lines.lines().toList()) {
            if (line.isBlank()) continue;
            JsonNode event = CodexJson.JSON.readTree(line);
            String type = event.path("type").asString("");
            if (completed || type.isEmpty() || type.equals("turn.failed") || type.equals("error")) {
                throw new IllegalStateException("Codex 事件流未正常结束。");
            }
            if (type.equals("item.completed") && event.path("item").path("type").asString("").equals("agent_message")) {
                lastMessage = event.path("item").get("text");
            }
            if (type.equals("turn.completed")) {
                completed = true;
                var usage = event.path("usage");
                JsonNode input = usage.get("input_tokens");
                JsonNode output = usage.get("output_tokens");
                if (input != null && output != null && input.isIntegralNumber() && output.isIntegralNumber()
                        && input.canConvertToLong() && output.canConvertToLong()
                        && input.longValue() >= 0 && output.longValue() >= 0) {
                    try { tokens = Math.addExact(input.longValue(), output.longValue()); }
                    catch (ArithmeticException ignored) { tokens = null; }
                }
            }
        }
        if (!completed || lastMessage == null || !lastMessage.isString()) {
            throw new IllegalStateException("Codex 未返回完整的最终回复。");
        }
        return new Response(CodexJson.JSON.readTree(lastMessage.asString()), tokens);
    }

    record Response(JsonNode value, Long tokens) { }

    private static final class DiagnosticBuffer {
        private static final int LIMIT = 64 * 1024;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private boolean truncated;

        void read(InputStream stream) throws IOException {
            try (stream) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = stream.read(buffer)) != -1) append(buffer, count);
            }
        }

        private synchronized void append(byte[] buffer, int count) {
            int kept = Math.min(count, LIMIT - bytes.size());
            bytes.write(buffer, 0, kept);
            truncated |= kept < count;
        }

        void appendFixed(String text) {
            byte[] buffer = text.getBytes(StandardCharsets.UTF_8);
            append(buffer, buffer.length);
        }

        synchronized String safeText() {
            return SecretRedactor.redact(bytes.toString(StandardCharsets.UTF_8))
                    + (truncated ? "\n[诊断已截断，最多保留 64 KiB]" : "");
        }
    }
}
