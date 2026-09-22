/*
 * 创建日期：2026-08-19
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证 CLI 内建命令、输出流、退出码和异常脱敏。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class CliApplicationTest {

    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String HELP_TEXT = String.join(
            LINE_SEPARATOR,
            "ZBAgentWF2.0 CLI",
            "用法: java -jar zbagentwf-cli-<version>.jar <command> [arguments]",
            "",
            "命令:",
            "  help, --help        显示帮助",
            "  version, --version  显示版本",
            "");

    @Test
    void noArgumentsShowsHelp() {
        assertResult(new String[0], () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_SUCCESS, HELP_TEXT, "");
    }

    @Test
    void helpCommandsShowHelp() {
        assertResult(new String[] {"help"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_SUCCESS, HELP_TEXT, "");
        assertResult(new String[] {"--help"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_SUCCESS, HELP_TEXT, "");
    }

    @Test
    void versionCommandsShowManifestVersion() {
        String expectedOutput = "ZBAgentWF2.0 0.1.0-SNAPSHOT" + LINE_SEPARATOR;
        assertResult(new String[] {"version"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_SUCCESS,
                expectedOutput, "");
        assertResult(new String[] {"--version"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_SUCCESS,
                expectedOutput, "");
    }

    @Test
    void missingManifestVersionIsExecutionFailure() {
        String expectedError = "命令执行失败：无法读取版本信息。" + LINE_SEPARATOR;
        assertResult(new String[] {"version"}, () -> null, CliApplication.EXIT_FAILURE, "", expectedError);
        assertResult(new String[] {"version"}, () -> "  ", CliApplication.EXIT_FAILURE, "", expectedError);
    }

    @Test
    void runtimeExceptionIsSanitized() {
        String expectedError = "命令执行失败：内部执行异常。" + LINE_SEPARATOR;
        assertResult(new String[] {"version"}, () -> {
            throw new IllegalStateException("sensitive detail");
        }, CliApplication.EXIT_FAILURE, "", expectedError);
    }

    @Test
    void unknownCommandDoesNotEchoInput() {
        String expectedError = "命令或参数错误：未知命令。" + LINE_SEPARATOR
                + "请运行 help 查看用法。" + LINE_SEPARATOR;
        assertResult(new String[] {"unknown\u001b[31m"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_USAGE,
                "", expectedError);
        assertResult(new String[] {""}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_USAGE, "", expectedError);
        assertResult(new String[] {"HELP"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_USAGE, "", expectedError);
    }

    @Test
    void knownCommandsRejectExtraArgumentsUsingCanonicalName() {
        String helpError = "命令或参数错误：help 不接受额外参数。" + LINE_SEPARATOR
                + "请运行 help 查看用法。" + LINE_SEPARATOR;
        String versionError = "命令或参数错误：version 不接受额外参数。" + LINE_SEPARATOR
                + "请运行 help 查看用法。" + LINE_SEPARATOR;
        assertResult(new String[] {"--help", "extra"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_USAGE,
                "", helpError);
        assertResult(new String[] {"--version", "extra"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_USAGE,
                "", versionError);
    }

    @Test
    void unknownFirstArgumentTakesPrecedenceOverArgumentCount() {
        String expectedError = "命令或参数错误：未知命令。" + LINE_SEPARATOR
                + "请运行 help 查看用法。" + LINE_SEPARATOR;
        assertResult(new String[] {"unknown", "extra"}, () -> "0.1.0-SNAPSHOT", CliApplication.EXIT_USAGE,
                "", expectedError);
    }

    private void assertResult(
            String[] args,
            Supplier<String> versionSupplier,
            int expectedExitCode,
            String expectedOutput,
            String expectedError) {
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
                PrintStream error = new PrintStream(errorBytes, true, StandardCharsets.UTF_8)) {
            int exitCode = new CliApplication(output, error, versionSupplier).run(args);
            assertAll(
                    () -> assertEquals(expectedExitCode, exitCode),
                    () -> assertEquals(expectedOutput, outputBytes.toString(StandardCharsets.UTF_8)),
                    () -> assertEquals(expectedError, errorBytes.toString(StandardCharsets.UTF_8)));
        }
    }
}
