# 文件索引

单工程：根pom负责构建，根REQUIREMENTS记录验收。以下均为实际Java源码。

| 文件 | 类型 |
| --- | --- |
| `src/main/java/com/kk24426/zbagentwf/ZbAgentWfApplication.java` | Web启动与生命周期 |
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
| `src/main/java/com/kk24426/zbagentwf/common/AgentBean.java` | Agent 数据骨架及属性访问 |
| `src/main/java/com/kk24426/zbagentwf/user/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/user/AgentBase.java` | 用户抽象类占位，未定义能力 |
| `src/main/java/com/kk24426/zbagentwf/user/UserImpl.java` | 用户接口的空父接口 |
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
| `src/test/java/com/kk24426/zbagentwf/user/UserImplTest.java` | 父接口关系测试，不进入JAR |

入口ZbAgentWfApplication负责Web初始化与生命周期；CLI类和测试已移除。
首页资源为src/main/resources/static/index.html、app.css、favicon.svg和chat.js；脚本提交单次请求，结果只作为纯文本显示。
agent.persistence负责可选MySQL配置与无参数SQL诊断；common.logging负责日志路径、脱敏、故障可见性。
测试包含聊天调用替身、跨包注入、日志滚动、真实JAR未接入路径、显式MySQL专用库验收。package-info仅职责标记。

规则入口：根AGENTS.md、三个包AGENTS.md、根REQUIREMENTS.md。
流程入口：[开发技能](../../.agents/skills/zb-development/SKILL.md)与[审核技能](../../.agents/skills/zb-review/SKILL.md)；审核模板随审核技能维护。
