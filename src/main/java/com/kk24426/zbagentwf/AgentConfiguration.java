/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：读取外部模型注册配置并装配发现、工厂和项目服务。
 */
package com.kk24426.zbagentwf;

import com.kk24426.zbagentwf.agent.project.ProjectDomainImpl;
import com.kk24426.zbagentwf.agent.persistence.ProjectDao;
import com.kk24426.zbagentwf.agent.persistence.mapper.ProjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import com.kk24426.zbagentwf.agent.registry.*;
import com.kk24426.zbagentwf.common.project.config.ProjectSettings;
import com.kk24426.zbagentwf.user.project.api.ProjectDomain;

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
    /**
     * 按配置条目键合并外部模型注册并装配目录；Spring随后显式初始化本机可用性，不在这里运行模型。
     */
    @Bean(initMethod = "initialize")
    AgentCatalog agentCatalog(Environment environment) {
        // 使用配置条目键合并各来源字段，避免 Spring 列表覆盖导致单字段覆盖丢失其它必填值。
        var definitions = Binder.get(environment).bind("zb.agents", Bindable.mapOf(String.class, AgentDefinition.class))
                .orElse(Map.of());
        // 空注册表允许仅启动 Web；真正获取模型时由目录报告无可用项，不擅自选择默认模型。
        return new AgentCatalog(new ArrayList<>(definitions.values()));
    }

    /**
     * 启动时显式读取工作目录config/prompts中的固定规则文件；保留缺失状态，实际调用时检查必需规则。
     */
    @Bean
    PromptCatalog promptCatalog() throws IOException { return PromptCatalog.load(Path.of("config", "prompts")); }

    /** 创建当前应用独有的内存索引，重启后不恢复实体。 */
    @Bean
    MemoryStore memoryStore() { return new MemoryStore(); }

    /**
     * 装配三角色默认选择的启动快照；字段缺失延后到使用时拒绝，不凭模型列表推断默认值。
     */
    @Bean
    RoleAgentResolver roleAgentResolver(Environment environment, PromptCatalog prompts) {
        var roles = Binder.get(environment).bind("zb.agent-roles", Bindable.mapOf(String.class, RoleAgentResolver.Selection.class))
                .orElse(Map.of());
        return new RoleAgentResolver(roles, prompts);
    }

    /** 创建应用共享执行器工厂，Spring销毁时统一回收执行资源；构造不运行模型。 */
    @Bean(destroyMethod = "close")
    AgentExecutorFactoryImpl agentExecFactory(AgentCatalog catalog, PromptCatalog prompts, ProjectSettings settings, ProjectDao dao) { return new AgentExecutorFactoryImpl(catalog, prompts, settings, dao); }

    /** 未启用mysql时仍能装配页面，但所有项目操作明确报告503，不使用内存替代数据库。 */
    @Bean
    ProjectDao projectDao(ObjectProvider<ProjectMapper> mapper,ObjectProvider<PlatformTransactionManager> transactions) {
        return new ProjectDao(mapper.getIfAvailable(),transactions.getIfAvailable());
    }

    /** 将规划入口绑定到当前工厂，仅使用该工厂登记的执行器。 */
    @Bean
    AgentRequirementPlanner agentRequirementPlanner(AgentExecutorFactoryImpl factory) { return new AgentRequirementPlanner(factory); }

    /** 组合项目实现所需目录配置、模型工厂、规划、数据库聚合与角色选择，不在装配时创建项目目录。 */
    @Bean
    ProjectDomain projectDomain(ProjectSettings settings, AgentExecutorFactoryImpl factory, AgentRequirementPlanner planner,
            ProjectDao dao, RoleAgentResolver roles) {
        return new ProjectDomainImpl(settings, factory, planner, dao, roles);
    }

    /**
     * 在容器关闭事件阶段先停止Agent受理并中断工作，后续工厂销毁继续使用同一截止时间完成有界收尾。
     */
    @Bean
    ApplicationListener<ContextClosedEvent> agentShutdownListener(AgentExecutorFactoryImpl factory) {
        // 关闭事件先停止受理并中断工作，后续 Bean 销毁再有限等待；两阶段使用同一个资源截止时间。
        return event -> factory.beginShutdown();
    }
}
