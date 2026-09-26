/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：读取外部模型注册配置并装配发现、工厂和项目服务。
 */
package com.kk24426.zbagentwf;

import com.kk24426.zbagentwf.agent.project.ProjectUserifImpl;
import com.kk24426.zbagentwf.agent.registry.*;
import com.kk24426.zbagentwf.common.project.bean.ProjectSettings;
import com.kk24426.zbagentwf.user.project.domain.ProjectDomain;

import java.util.ArrayList;
import java.nio.file.Path;
import java.io.IOException;
import com.kk24426.zbagentwf.common.memory.MemoryStore;
import com.kk24426.zbagentwf.agent.prompt.PromptCatalog;
import java.util.Map;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.*;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;

/** 初始化只检查注册配置和本机文件，不运行模型、不创建业务项目。 */
@Configuration(proxyBeanMethods = false)
public class AgentConfiguration {
    @Bean(initMethod = "initialize")
    AgentCatalog agentCatalog(Environment environment) {
        // 使用配置条目键合并各来源字段，避免 Spring 列表覆盖导致单字段覆盖丢失其它必填值。
        var definitions = Binder.get(environment).bind("zb.agents", Bindable.mapOf(String.class, AgentDefinition.class))
                .orElse(Map.of());
        // 空注册表允许仅启动 Web；真正获取模型时由目录报告无可用项，不擅自选择默认模型。
        return new AgentCatalog(new ArrayList<>(definitions.values()));
    }

    @Bean
    PromptCatalog promptCatalog() throws IOException { return PromptCatalog.load(Path.of("config", "prompts")); }

    @Bean
    MemoryStore memoryStore() { return new MemoryStore(); }

    @Bean
    RoleAgentResolver roleAgentResolver(Environment environment, PromptCatalog prompts) {
        var roles = Binder.get(environment).bind("zb.agent-roles", Bindable.mapOf(String.class, RoleAgentResolver.Selection.class))
                .orElse(Map.of());
        return new RoleAgentResolver(roles, prompts);
    }

    @Bean(destroyMethod = "close")
    AgentExecFactoryImpl agentExecFactory(AgentCatalog catalog, PromptCatalog prompts) { return new AgentExecFactoryImpl(catalog, prompts); }

    @Bean
    AgentRequirementPlanner agentRequirementPlanner(AgentExecFactoryImpl factory) { return new AgentRequirementPlanner(factory); }

    @Bean
    ProjectDomain projectDomain(ProjectSettings settings, AgentExecFactoryImpl factory, AgentRequirementPlanner planner,
            MemoryStore memory, RoleAgentResolver roles) {
        return new ProjectUserifImpl(settings, factory, planner, memory, roles);
    }

    @Bean
    ApplicationListener<ContextClosedEvent> agentShutdownListener(AgentExecFactoryImpl factory) {
        // 关闭事件先停止受理并中断工作，后续 Bean 销毁再有限等待；两阶段使用同一个资源截止时间。
        return event -> factory.beginShutdown();
    }
}
