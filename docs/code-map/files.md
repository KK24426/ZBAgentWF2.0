# 文件索引

单工程：根pom负责构建，根REQUIREMENTS记录验收。以下均为实际Java源码。

| 文件 | 类型 |
| --- | --- |
| `src/main/java/com/kk24426/zbagentwf/CliApplication.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/ZbAgentWfCli.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/MySqlConfiguration.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/SqlDiagnosticsInterceptor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/mapper/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/LogFailureMonitor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/RunLogging.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/SanitizingEncoder.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/SecretRedactor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/user/package-info.java` | 运行代码 |
| `src/test/java/com/kk24426/zbagentwf/CliApplicationTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/CliJarIT.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/ContextTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/MySqlIT.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/InjectionImplementation.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/persistence/mapper/ProbeMapper.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/common/logging/LoggingTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/user/InjectionFixture.java` | 测试，不进入JAR |

入口ZbAgentWfCli负责初始化/退出，CliApplication负责命令。
agent.persistence负责可选MySQL配置与无参数SQL诊断；common.logging负责日志路径、脱敏、故障可见性。
测试包含跨包注入、日志滚动、真实JAR、显式MySQL专用库验收。package-info仅职责标记。

规则入口：根AGENTS.md、三个包AGENTS.md、根REQUIREMENTS.md。
流程入口：docs/operations/agent-workflow.md和docs/templates/review/。
