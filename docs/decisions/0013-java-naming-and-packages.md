# ADR 0013：Java 命名与包目录统一

日期：2026-09-27；状态：用户已批准，按两个阶段实施。

用户要求先完成类名、方法名修复，再调整包目录。保留单 Maven 工程及 user、agent、common 的所有权和职责边界。

## 第一阶段：类型与方法命名

- ProjectUserifImpl 改为 ProjectDomainImpl，AgentExecutorImpl 改为 AbstractAgentExecutor，AgentBase 改为 AgentRegistry；抽象形态和业务方法保持不变。
- CodexAgentExec、AgentExecFactory/Impl 分别改为 CodexAgentExecutor、AgentExecutorFactory/Impl；AgentExecCallback、AgentExecResult 改为 AgentExecutionCallback、AgentExecutionResult。
- getDefluatPrompt 修正为 getDefaultPrompt；相关测试随类型更名，ContextTest 改为 ApplicationContextTest。
- AgentTypeEnum 改为 AgentRole，常量使用 PLANNING、DEVELOPMENT、REVIEW。显式 configKey()/fromConfigKey() 保留 planning、development、review 配置键及提示词文件名称，仍精确匹配并拒绝非法名称。
- ProjectHttp 保留嵌套结构，Create、Content、View 改为 CreateProjectRequest、ProjectContentRequest、ProjectResponse；HTTP 路径、字段及快照逻辑保持不变。

第一阶段保留包位置，在完成验证、独立审核和独立提交后进入第二阶段；不提供旧名称兼容层。Java 调用方需同步类型名和 import；应用不承诺 Maven 类库兼容。历史 ADR 中的旧名称保留为决策背景，当前名称以源码、契约和 code map 为准。

## 第二阶段：包目录

第二阶段另行审核包迁移计划，按下表调整现有类型；迁移不改变业务行为、配置契约或所有权。

| 原位置 | 新位置 |
| --- | --- |
| user.agent.userif | user.agent.api |
| user.project.domain.ProjectDomain | user.project.api.ProjectDomain |
| user.chat.service.AgentChat | user.chat.api.AgentChat |
| common.agent.bean | common.agent.model |
| common.project.bean 中的项目、需求、Task、状态及角色 | common.project.model |
| common.project.bean.ProjectSettings | common.project.config.ProjectSettings |
| common.project.bean.ProjectHttp | common.project.dto.ProjectHttp |
| common.chat 的请求与响应 | common.chat.dto |
| agent.AbstractAgentExecutor | agent.runtime.AbstractAgentExecutor |

对应测试跟随被测类型归包，ChatAgentFixture 位于 user.chat.api。保留 ProjectHttp 嵌套数据类型及原有字段；补充必要 import，不提高成员可见性。根启动与配置类、Mapper/XML、日志配置保持原位；用户需要按新 import 调用，旧包不提供兼容类。
