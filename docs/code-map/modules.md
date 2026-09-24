# 包路由
单 Maven 工程，文件名为历史入口。

| 任务 | 位置 | 验证 |
| --- | --- | --- |
| 用户接口、编排 | user | 目标测试、调用方、verify |
| 项目/需求/Task、异步执行声明 | common.project.bean、common.agent.bean、user.project.userif、user.agent.userif | ProjectModelTest、AgentExecContractTest、verify；替身不代表真实执行 |
| Codex 进程与异步执行、只读规划 | agent.codex | CodexAgentExecTest、CodexRequirementPlannerTest；真实 Java 子进程 fixture，不等于真实模型验收 |
| UUID 项目、追加需求、串行 Task | agent.project | ProjectUserifImplTest；显式组合，模型发现/应用装配待确认 |
| 必填项目根目录初始化 | 根包ProjectConfiguration、common.project.bean.ProjectSettings、外部config/project.properties | ProjectConfigurationTest、ContextTest、真实WebJarIT配置加载/覆盖/失败 |
| 接口与数据库实现 | agent | 目标测试、调用方、verify；数据库用mysql-it |
| 单次聊天骨架 | user.chat、agent.chat、common.chat、common.exception、static/chat.js | Service/Controller替身、过滤器/日志、真实JAR未接入路径 |
| Bean、工具、共享技术异常 | common | 目标测试、调用方、verify |
| Web启动、首页、配置 | 根包、agent.web及resources | 单元测试、真实Web JAR IT、verify |

开始任务先读根AGENTS.md、就近AGENTS.md和根REQUIREMENTS.md。
