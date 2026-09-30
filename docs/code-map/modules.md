# 包路由
单 Maven 工程，文件名为历史入口。

| 任务 | 位置 | 验证 |
| --- | --- | --- |
| 用户接口、编排 | user | 目标测试、调用方、verify |
| 项目/需求/Task、异步执行声明 | common.project.model、common.agent.model、user.project.api、user.agent.api | ProjectModelTest、AgentExecutionContractTest、verify；替身不代表真实执行 |
| 项目应用/模块/功能、变更计划与履历骨架 | common.project.model、user.project.api | ProjectElementSnapshotTest、test-compile、verify；仅类型和抽象契约，业务与持久化尚未实现 |
| Codex 进程与异步执行、只读规划 | agent.codex | CodexAgentExecutorTest、CodexRequirementPlannerTest；真实 Java 子进程 fixture，不等于真实模型验收 |
| 持久化项目、两阶段需求、串行 Task | agent.project | ProjectDomainImplTest、AgentRegistryTest；三角色绑定与隔离 |
| 模型配置、工厂与资源 | 根包AgentConfiguration、agent.registry、agent.runtime、config/agents.properties | AgentConfigurationTest、AgentRegistryTest、ExecutionResourcesTest |
| 必填项目根目录初始化 | 根包ProjectConfiguration、common.project.config.ProjectSettings、外部config/project.properties | ProjectConfigurationTest、ApplicationContextTest、真实WebJarIT配置加载/覆盖/失败 |
| 接口与数据库实现 | agent | 目标测试、调用方、verify；数据库用mysql-it |
| 单次聊天骨架 | agent.chat.controller、user.chat.service/api、agent.chat、common.chat.dto、common.exception、static/chat.js | Service/Controller替身、过滤器/日志、真实JAR未接入路径 |
| Bean、工具、共享技术异常 | common | 目标测试、调用方、verify |
| Web启动、首页、配置 | 根包、agent.web及resources | 单元测试、真实Web JAR IT、verify |
| 页面与HTTP消息本地化 | MsgConfiguration、common.msg、agent.web、resources/msg、resources/web、static/chat.js | MsgCatalogTest、MsgConfigurationTest、MsgLocaleResolverTest、HomePageControllerTest、Filter/Controller测试、WebJarIT与浏览器验收 |

开始任务先读根AGENTS.md、就近AGENTS.md和根REQUIREMENTS.md。

新增路由：通用内存/快照看common.memory、common.project.dto（MemoryStoreTest/ProjectHttpTest）；规则与绑定看agent.prompt、agent.runtime.AbstractAgentExecutor、RoleAgentResolver（AgentPromptTest/AgentRegistryTest）；项目HTTP看agent.project.controller、user.project.service（ProjectControllerTest/WebJarIT）。

项目工作台：agent.web.ProjectPageController/LocalizedPageRenderer、web/projects.html、static/projects.js/projects.css/messages.js；验证ProjectPageControllerTest、WebJarIT及src/test/browser项目行为回归。共享渲染和语言选择同时回归聊天首页。

持久化：agent.persistence.ProjectDao/ProjectMapper/XML/DDL → ProjectPersistenceIT；安全审核：agent.codex.CodexClient/Prompt → CodexSafetyTest及CodexAgentExecutorTest。根AgentConfiguration在mysql未启用时装配明确未就绪DAO，项目无内存降级。
