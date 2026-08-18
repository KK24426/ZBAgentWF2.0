/*
 * 创建日期：2026-08-19
 * 更新日期：2026-08-19
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：提供 ZBAgentWF2.0 CLI 的公开进程入口。
 */
package com.kk24426.zbagentwf.runtime;

/**
 * ZBAgentWF2.0 CLI 进程入口。
 */
public final class ZbAgentWfCli {

    private ZbAgentWfCli() {
    }

    /**
     * 解析命令并以约定退出码结束进程。
     *
     * @param args CLI 参数
     */
    public static void main(String[] args) {
        CliApplication application = new CliApplication(
                System.out,
                System.err,
                () -> ZbAgentWfCli.class.getPackage().getImplementationVersion());
        int exitCode = application.run(args);
        if (exitCode != CliApplication.EXIT_SUCCESS) {
            System.exit(exitCode);
        }
    }
}
