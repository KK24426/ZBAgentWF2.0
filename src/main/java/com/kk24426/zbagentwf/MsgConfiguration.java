/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：启动时显式读取消息和页面模板，装配请求语言解析。
 */
package com.kk24426.zbagentwf;

import com.kk24426.zbagentwf.agent.web.MsgLocaleResolver;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

/** 本地化配置修改后重启；不创建配置文件，不添加热加载线程。 */
@Configuration(proxyBeanMethods = false)
public class MsgConfiguration {
    /**
     * 启动时加载内置三语和工作目录config/msg的可选覆盖；读取或校验失败阻止初始化，不创建配置。
     */
    @Bean
    MsgCatalog msgCatalog() throws IOException {
        return MsgCatalog.load(Path.of("config", "msg"));
    }

    /**
     * 读取服务级语言选择，未配置时使用auto并交给解析器校验；不读取系统默认语言。
     */
    @Bean(name = "localeResolver")
    MsgLocaleResolver localeResolver(Environment environment) {
        return new MsgLocaleResolver(environment.getProperty("zb.msg.locale", "auto"));
    }

    /**
     * 从非公开classpath位置以UTF-8读取聊天首页模板；缺失或读取失败直接阻止启动。
     */
    @Bean(name = "homePageTemplate")
    String homePageTemplate() throws IOException {
        return new ClassPathResource("web/index.html").getContentAsString(StandardCharsets.UTF_8);
    }
    /**
     * 从非公开classpath位置以UTF-8读取项目工作台模板；模板经页面渲染器校验后使用。
     */
    @Bean(name = "projectPageTemplate")
    String projectPageTemplate() throws IOException {
        return new ClassPathResource("web/projects.html").getContentAsString(StandardCharsets.UTF_8);
    }
}
