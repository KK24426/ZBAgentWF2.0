# ADR 0013：Java 命名与包目录统一

日期：2026-09-27；状态：用户已批准，分阶段实施。

用户要求先完成类名、方法名修复，再调整包目录。保留单 Maven 工程及 user、agent、common 的所有权和职责边界。

## 第一阶段：类型与方法命名

- ProjectUserifImpl 改为 ProjectDomainImpl，AgentExecutorImpl 改为 AbstractAgentExecutor，AgentBase 改为 AgentRegistry；抽象形态和业务方法保持不变。
- CodexAgentExec、AgentExecFactory/Impl 分别改为 CodexAgentExecutor、AgentExecutorFactory/Impl；AgentExecCallback、AgentExecResult 改为 AgentExecutionCallback、AgentExecutionResult。
- getDefluatPrompt 修正为 getDefaultPrompt；相关测试随类型更名，ContextTest 改为 ApplicationContextTest。
- AgentTypeEnum 改为 AgentRole，常量使用 PLANNING、DEVELOPMENT、REVIEW。显式 configKey()/fromConfigKey() 保留 planning、development、review 配置键及提示词文件名称，仍精确匹配并拒绝非法名称。
- ProjectHttp 保留嵌套结构，Create、Content、View 改为 CreateProjectRequest、ProjectContentRequest、ProjectResponse；HTTP 路径、字段及快照逻辑保持不变。

当前阶段保留包位置，不提供旧名称兼容层。Java 调用方需同步类型名和 import；应用不承诺 Maven 类库兼容。历史 ADR 中的旧名称保留为决策背景，当前名称以源码、契约和 code map 为准。

## 第二阶段：包目录

第一阶段完成验证和审核后，另行审核包迁移计划。用户已批准的方向为 user.<业务>.api、common.<业务>.model/dto/config，以及 agent.runtime 中的执行器公共实现；迁移不改变业务行为、配置契约或所有权。
