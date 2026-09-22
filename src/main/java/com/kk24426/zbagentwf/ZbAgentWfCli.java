/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：初始化 Spring、日志和 CLI，并管理进程退出。
 */
package com.kk24426.zbagentwf;

import ch.qos.logback.classic.LoggerContext;
import com.kk24426.zbagentwf.common.logging.LogFailureMonitor;
import com.kk24426.zbagentwf.common.logging.RunLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/** 可在 Eclipse 直接运行，也可通过 java -jar 启动；根包覆盖三个职责包。 */
@SpringBootApplication(proxyBeanMethods = false, exclude = DataSourceAutoConfiguration.class)
public class ZbAgentWfCli {
    /** CLI 参数只用于命令解析，不作为 Spring 配置来源。 */
    public static void main(String[] args) {
        int exitCode = launch(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int launch(String[] args) {
        // 必须在首次取得 logger 和 Boot 初始化前准备路径；此阶段故障只能写 stderr。
        try {
            RunLogging.prepare();
        } catch (Exception failure) {
            System.err.println("启动失败：无法初始化日志目录或文件。");
            return 1;
        }
        SpringApplication application = new SpringApplication(ZbAgentWfCli.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAddCommandLineProperties(false);
        application.setLogStartupInfo(false); // 自行记录启动，不输出操作系统账号和完整启动路径。
        application.setBannerMode(Banner.Mode.LOG);
        application.addInitializers(ignored -> LoggerFactory.getLogger(ZbAgentWfCli.class)
                .info("应用初始化开始"));
        ConfigurableApplicationContext context = null;
        Logger logger = null;
        long started = System.nanoTime();
        int exitCode = 1;
        try {
            context = application.run(); // 不把未经检查的业务参数交给框架。
            logger = LoggerFactory.getLogger(ZbAgentWfCli.class);
            logger.info("容器初始化完成，命令开始 command={}", safeCommand(args));
            exitCode = context.getBean(CliApplication.class).run(args);
        } catch (Exception failure) {
            logger = LoggerFactory.getLogger(ZbAgentWfCli.class);
            logger.error("初始化或执行失败", failure);
            System.err.println("命令执行失败：内部执行异常。");
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (RuntimeException failure) {
                    exitCode = 1;
                    logger.error("容器关闭失败", failure);
                }
            }
            if (LogFailureMonitor.hasFailed()) {
                exitCode = 1;
            }
            if (logger != null) {
                logger.info("运行结束 exitCode={} elapsedMs={}", exitCode,
                        (System.nanoTime() - started) / 1_000_000);
            }
            // 同步 appender 停止时刷新尾部记录；压缩任务也由 Logback 完成关闭。
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
            if (LogFailureMonitor.hasFailed()) {
                exitCode = 1;
            }
        }
        return exitCode;
    }

    private static String safeCommand(String[] args) {
        if (args == null || args.length == 0) {
            return "help";
        }
        return switch (args[0]) {
            case "help", "--help" -> "help";
            case "version", "--version" -> "version";
            default -> "unknown"; // 未知输入不进入日志。
        };
    }

    @Bean
    CliApplication cliApplication() {
        return new CliApplication(System.out, System.err,
                () -> ZbAgentWfCli.class.getPackage().getImplementationVersion());
    }
}
