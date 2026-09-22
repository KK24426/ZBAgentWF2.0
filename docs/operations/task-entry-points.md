<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按任务类型路由开始实施前必须阅读的规则和事实文档。
 -->

# Task Entry Points

所有任务先读根 `AGENTS.md`；涉及具体目录时再读最近的 `AGENTS.md` 和根 `REQUIREMENTS.md`。真实代码和 Maven 配置是最终依据。

| 任务类型 | 必读资料 | 额外检查 |
| --- | --- | --- |
| 架构、模块和依赖 | `docs/README.md`、`docs/architecture/overview.md`、`docs/architecture/module-boundaries.md`、`docs/code-map/modules.md`、相关 ADR | 用户批准、调用方向、code map |
| 公共接口、DTO、状态和错误语义 | `docs/contracts/module-ports.md`、`docs/contracts/open-issues.md`、目标包源码公开出口和 `REQUIREMENTS.md` | 用户批准、调用方、兼容性、文档同步 |
| 功能编码 | `docs/AI_DEV_GUIDE.md`、目标包 `AGENTS.md`、`REQUIREMENTS.md`、相关接口和测试 | 修改范围、验收、分层验证 |
| 测试 | 目标源码、现有测试、目标包 `REQUIREMENTS.md`、`docs/operations/quality-gates.md` | 主路径、失败路径、回归路径 |
| bugfix | 报错点、同模块调用点、直接调用方、相关契约与测试 | 影响半径、共享状态、错误映射 |
| 代码或结果审查 | `docs/templates/review/`、计划/实现 diff、验证证据、`docs/operations/agent-workflow.md` | reviewer 独立性、基准 SHA、staged hash |
| 构建和本地环境 | `pom.xml`、`.mvn/wrapper/maven-wrapper.properties`、`docs/operations/local-dev.md` | 实际 Java/Maven 版本、显式目标环境 |
| 数据或外部副作用 | `docs/operations/asset-policy.md`、相关契约、目标包规则 | 环境、配置来源、权限、脱敏、停止条件 |

无法从表中确定任务边界时停止并请用户确认，不得用新增抽象或“通用模块”掩盖职责不清。
