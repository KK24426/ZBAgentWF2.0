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
静态首页资源为src/main/resources/static/index.html、app.css和favicon.svg。
agent.persistence负责可选MySQL配置与无参数SQL诊断；common.logging负责日志路径、脱敏、故障可见性。
测试包含跨包注入、日志滚动、真实JAR、显式MySQL专用库验收。package-info仅职责标记。

规则入口：根AGENTS.md、三个包AGENTS.md、根REQUIREMENTS.md。
流程入口：docs/operations/agent-workflow.md和docs/templates/review/。
