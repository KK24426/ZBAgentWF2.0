/*
 * 创建日期：2026-08-19
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：实现 CLI 内建命令解析、输出和退出码映射。
 */
package com.kk24426.zbagentwf;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 包内 CLI 执行器，将命令结果转换为稳定的输出和退出码。
 */
final class CliApplication {
    private static final Logger LOG = LoggerFactory.getLogger(CliApplication.class);

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_FAILURE = 1;
    static final int EXIT_USAGE = 2;

    private static final String HELP_TEXT = String.join(
            System.lineSeparator(),
            "ZBAgentWF2.0 CLI",
            "用法: java -jar zbagentwf-cli-<version>.jar <command> [arguments]",
            "",
            "命令:",
            "  help, --help        显示帮助",
            "  version, --version  显示版本",
            "");
    private static final String HELP_HINT = "请运行 help 查看用法。";

    private final PrintStream standardOutput;
    private final PrintStream errorOutput;
    private final Supplier<String> versionSupplier;

    CliApplication(PrintStream standardOutput, PrintStream errorOutput, Supplier<String> versionSupplier) {
        this.standardOutput = Objects.requireNonNull(standardOutput, "standardOutput");
        this.errorOutput = Objects.requireNonNull(errorOutput, "errorOutput");
        this.versionSupplier = Objects.requireNonNull(versionSupplier, "versionSupplier");
    }

    int run(String[] args) {
        try {
            return execute(Objects.requireNonNull(args, "args"));
        } catch (RuntimeException failure) {
            LOG.error("命令执行失败", failure);
            errorOutput.println("命令执行失败：内部执行异常。");
            return EXIT_FAILURE;
        }
    }

    private int execute(String[] args) {
        if (args.length == 0) {
            return showHelp();
        }

        String command = args[0];
        if (isHelpCommand(command)) {
            return args.length == 1 ? showHelp() : rejectExtraArguments("help");
        }
        if (isVersionCommand(command)) {
            return args.length == 1 ? showVersion() : rejectExtraArguments("version");
        }
        return rejectUnknownCommand();
    }

    private int showHelp() {
        standardOutput.print(HELP_TEXT);
        return EXIT_SUCCESS;
    }

    private int showVersion() {
        String version = versionSupplier.get();
        if (version == null || version.isBlank()) {
            errorOutput.println("命令执行失败：无法读取版本信息。");
            return EXIT_FAILURE;
        }
        standardOutput.printf("ZBAgentWF2.0 %s%n", version);
        return EXIT_SUCCESS;
    }

    private int rejectExtraArguments(String canonicalCommand) {
        errorOutput.printf("命令或参数错误：%s 不接受额外参数。%n", canonicalCommand);
        errorOutput.println(HELP_HINT);
        return EXIT_USAGE;
    }

    private int rejectUnknownCommand() {
        errorOutput.println("命令或参数错误：未知命令。");
        errorOutput.println(HELP_HINT);
        return EXIT_USAGE;
    }

    private boolean isHelpCommand(String command) {
        return "help".equals(command) || "--help".equals(command);
    }

    private boolean isVersionCommand(String command) {
        return "version".equals(command) || "--version".equals(command);
    }
}
