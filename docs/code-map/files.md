# 文件索引

单工程：根pom负责构建，根REQUIREMENTS记录验收。以下均为实际Java源码。

| 文件 | 类型 |
| --- | --- |
| `src/main/java/com/kk24426/zbagentwf/ZbAgentWfApplication.java` | Web启动与生命周期 |
| `src/main/java/com/kk24426/zbagentwf/ProjectConfiguration.java` | 必填项目根目录读取与初始化校验，不创建目录 |
| `src/main/java/com/kk24426/zbagentwf/agent/web/WebRequestFilter.java` | 请求保护与安全日志 |
| `src/main/java/com/kk24426/zbagentwf/agent/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/MySqlConfiguration.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/SqlDiagnosticsInterceptor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/mapper/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/LogFailureMonitor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/RunLogging.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/SanitizingEncoder.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/SecretRedactor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/agent/bean/AgentBean.java` | Agent 数据骨架及属性访问 |
| `src/main/java/com/kk24426/zbagentwf/common/agent/bean/AgentExecResult.java` | 单次执行结果及待确认内容 |
| `src/main/java/com/kk24426/zbagentwf/common/project/bean/Project.java` | 项目标识、工作目录、三角色执行器和需求列表 |
| `src/main/java/com/kk24426/zbagentwf/common/project/bean/Requirement.java` | 项目内需求及Task列表 |
| `src/main/java/com/kk24426/zbagentwf/common/project/bean/RequirementTask.java` | 规划任务、状态与单次执行结果 |
| `src/main/java/com/kk24426/zbagentwf/common/project/bean/TaskStatus.java` | 已批准的任务状态值，不实现状态转换 |
| `src/main/java/com/kk24426/zbagentwf/common/project/bean/ProjectSettings.java` | 初始化后的只读项目根目录 |
| `src/main/java/com/kk24426/zbagentwf/user/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/userif/AgentBase.java` | 本机Agent发现、查询和刷新抽象契约 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/userif/AgentExecutor.java` | 项目内单次异步执行与诊断查询契约 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/userif/AgentExecCallback.java` | 执行最终结果回调契约 |
| `src/main/java/com/kk24426/zbagentwf/user/project/userif/ProjectUserif.java` | 项目创建、需求规划和任务结果汇总契约 |
| `src/main/java/com/kk24426/zbagentwf/user/UserInterface.java` | 用户接口的空父接口 |
| `src/main/java/com/kk24426/zbagentwf/user/UserService.java` | 用户服务父类占位，未定义行为 |
| `src/main/java/com/kk24426/zbagentwf/user/chat/controller/ChatController.java` | 聊天HTTP入口与固定错误映射 |
| `src/main/java/com/kk24426/zbagentwf/user/chat/service/ChatService.java` | 用户侧请求校验与Agent调用编排 |
| `src/main/java/com/kk24426/zbagentwf/user/chat/service/AgentChat.java` | 单次Agent调用公共接口 |
| `src/main/java/com/kk24426/zbagentwf/common/exception/AgentUnavailableException.java` | Agent实现与调用层共用的不可用异常 |
| `src/main/java/com/kk24426/zbagentwf/agent/chat/AgentChatImpl.java` | 正式Agent占位实现，明确不可用 |
| `src/main/java/com/kk24426/zbagentwf/common/chat/ChatRequest.java` | 聊天请求与字段类型绑定 |
| `src/main/java/com/kk24426/zbagentwf/common/chat/ChatResponse.java` | 聊天回复数据载体 |
| `src/test/java/com/kk24426/zbagentwf/user/chat/service/ChatAgentFixture.java` | 可控Agent测试替身，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/user/chat/service/ChatServiceTest.java` | 输入边界、原样委派、生产占位测试 |
| `src/test/java/com/kk24426/zbagentwf/user/chat/controller/ChatControllerTest.java` | Controller/Service/替身HTTP与隐私失败路径测试 |
| `src/test/java/com/kk24426/zbagentwf/agent/web/WebRequestFilterTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/WebJarIT.java` | 真实Web JAR测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/ContextTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/MySqlIT.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/InjectionImplementation.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/persistence/mapper/ProbeMapper.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/common/logging/LoggingTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/user/InjectionFixture.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/common/AgentBeanTest.java` | 属性契约测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/user/UserInterfaceTest.java` | 父接口关系测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/common/project/bean/ProjectModelTest.java` | 多层包含、独立列表与属性契约 |
| `src/test/java/com/kk24426/zbagentwf/user/agent/userif/AgentExecContractTest.java` | 测试替身的项目传递、执行标识及回调关联 |
| `src/test/java/com/kk24426/zbagentwf/ProjectConfigurationTest.java` | 必填目录、规范化、错误与无建目录副作用 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexAgentExec.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexClient.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexJson.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexRequirementPlanner.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexSchemas.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/project/ProjectUserifImpl.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/CodexAgentExecTest.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/CodexFixtureSupport.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/CodexRequirementPlannerTest.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/FakeCodexFixture.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/project/ProjectUserifImplTest.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentCatalog.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentDefinition.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentExecFactoryImpl.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentRequirementPlanner.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/runtime/ExecutionResources.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/AgentConfiguration.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/userif/AgentExecFactory.java` | 模型注册、工厂装配或共享资源实现 |
| `src/test/java/com/kk24426/zbagentwf/agent/registry/AgentRegistryTest.java` | 测试，不进入正式JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/runtime/ExecutionResourcesTest.java` | 测试，不进入正式JAR |
| `src/test/java/com/kk24426/zbagentwf/AgentConfigurationTest.java` | 测试，不进入正式JAR |

入口ZbAgentWfApplication负责Web初始化与生命周期；CLI类和测试已移除。
ProjectConfiguration通过application.properties导入启动工作目录下的config/project.properties；config/project.properties.example仅为入仓示例，真实配置不入仓。
首页资源为src/main/resources/static/index.html、app.css、favicon.svg和chat.js；脚本提交单次请求，结果只作为纯文本显示。
agent.persistence负责可选MySQL配置与无参数SQL诊断；common.logging负责日志路径、脱敏、故障可见性。
测试包含聊天调用替身、跨包注入、日志滚动、真实JAR未接入路径、显式MySQL专用库验收。package-info仅职责标记。

规则入口：根AGENTS.md、三个包AGENTS.md、根REQUIREMENTS.md。
流程入口：[开发技能](../../.agents/skills/zb-development/SKILL.md)与[审核技能](../../.agents/skills/zb-review/SKILL.md)；审核模板随审核技能维护。
