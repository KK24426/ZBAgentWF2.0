/*
 * 创建日期：2026-09-23
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：初始化日志与常驻 Web 服务，由 Spring 管理正常停机。
 */
package com.kk24426.zbagentwf;

import ch.qos.logback.classic.LoggerContext;
import com.kk24426.zbagentwf.common.logging.LogFailureMonitor;
import com.kk24426.zbagentwf.common.logging.RunLogging;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

/** 唯一启动入口；无 CLI 命令，配置来自环境变量、JVM 属性或外部配置文件。 */
@SpringBootApplication(proxyBeanMethods = false, exclude = DataSourceAutoConfiguration.class)
public class ZbAgentWfApplication {
    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Web 服务不接受命令参数，请使用环境变量或外部配置。");
            System.exit(2);
            return;
        }
        try {
            RunLogging.prepare();
        } catch (Exception failure) {
            System.err.println("启动失败：无法初始化日志目录或文件。");
            System.exit(1);
            return;
        }
        SpringApplication application = new SpringApplication(ZbAgentWfApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setAddCommandLineProperties(false);
        application.setLogStartupInfo(false);
        application.setBannerMode(Banner.Mode.OFF);
        application.addInitializers(ignored -> LoggerFactory.getLogger(ZbAgentWfApplication.class)
                .info("Web 应用初始化开始"));
        application.addListeners(event -> {
            if (event instanceof ContextClosedEvent) {
                LoggerFactory.getLogger(ZbAgentWfApplication.class).info("Web 服务关闭，开始释放资源");
            }
        });
        ConfigurableApplicationContext context = null;
        try {
            context = application.run();
            if (LogFailureMonitor.hasFailed()) {
                throw new IllegalStateException("启动期间日志系统故障。");
            }
            LoggerFactory.getLogger(ZbAgentWfApplication.class).info("Web 服务就绪 port={}",
                    context.getEnvironment().getProperty("local.server.port"));
            if (LogFailureMonitor.hasFailed()) {
                throw new IllegalStateException("就绪日志写入失败。");
            }
            // 正常返回 main 不关闭上下文；Tomcat 持续服务，Spring shutdown hook 负责优雅停机。
        } catch (Exception failure) {
            LoggerFactory.getLogger(ZbAgentWfApplication.class).error("Web 服务启动失败", failure);
            if (context != null) {
                try {
                    context.close();
                } catch (RuntimeException closeFailure) {
                    LoggerFactory.getLogger(ZbAgentWfApplication.class).error("启动失败后的资源释放异常", closeFailure);
                }
            }
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
            System.err.println("Web 服务启动失败，请检查运行日志。");
            System.exit(1);
        }
    }
}
