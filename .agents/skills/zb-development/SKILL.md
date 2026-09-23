---
name: zb-development
description: 在 ZBAgentWF2.0 中实现、修复、测试或调整工程流程时使用；准备方案、验证并交接已授权修改。只读计划仅执行分析阶段，独立审查使用 zb-review。
---

# 项目开发

遵守仓库根 [AGENTS.md](../../../AGENTS.md) 和就近规则；本技能不扩大授权。以下事实文档路径均相对仓库根；链接相对当前文件。主 Agent 统一协调和操作 Git，开发者提供方案、修改及验证证据。

## 接收任务与资料路由

先区分只读与授权写任务。读取根 REQUIREMENTS、用户原始需求和验收标准，检查 branch/upstream/HEAD/远端、完整 status（含 unstaged、staged、untracked、rename、delete）、两类 diff 及未跟踪文件内容。重点理解用户的接口、DTO、状态、错误、构建、配置和契约修改，形成影响摘要。边界或归属不明按根规则停止。

按任务读取下表对应资料，不默认读取所有文档：

| 任务 | 资料及检查 |
| --- | --- |
| 架构、包职责、依赖 | docs/README.md、docs/architecture/overview.md、docs/architecture/module-boundaries.md、docs/code-map/modules.md、相关 ADR；核对批准、调用方向和 code map |
| 接口、DTO、状态、错误 | docs/contracts/module-ports.md、docs/contracts/open-issues.md、目标包公开源码；核对批准、调用方和兼容性 |
| 功能、测试 | 目标包规则、接口、示例、测试意图；明确范围、验收与分层验证 |
| bugfix | 报错点、同包调用点、直接调用方、共享状态、错误映射、契约及回归路径 |
| 评审 | [审核技能](../zb-review/SKILL.md)、对应阶段模板、计划或 diff、验证证据 |
| 构建、环境 | pom.xml、.mvn/wrapper/maven-wrapper.properties、docs/operations/local-dev.md；核对实际版本和目标环境 |
| 数据、外部副作用 | docs/operations/asset-policy.md、相关契约和目标包规则；远程任务另读 docs/operations/remote-test.md；核对环境、配置来源、权限、脱敏和停止条件 |

说明修改范围、不涉及范围、用户已定义或批准的接口、验收标准、副作用目标和最小分层验证方案。只读任务到分析与报告结束。写任务由主 Agent 先按 [Git 流程](references/git-workflow.md) 处理用户 checkpoint，再把方案交独立 reviewer 使用审核技能；Plan 通过前不写项目文件，包括审核记录。

## 实现与验证

只实现批准边界内的最小功能，不预建抽象；adapter 不复制领域规则。公共契约以真实 Java 源码和测试为最终事实；发现接口不足按根规则提交影响及备选方案，不先改后报。

新增 Java 文件保留以下文件头并填写准确职责：

```java
/*
 * 创建日期：
 * 更新日期：
 * 做 成 者：zebiao
 * 版    本：
 * 功能概要：
 */
```

公共类型、关键分支、状态机、安全判断和副作用边界使用中文说明，简单代码不堆低价值注释。通用异常处理、网络、文件、数据访问保留扩展空间，共享抽取条件遵守根规则。

错误不得吞掉；适用时保留来源、所属包/组件、阶段、业务 ID、退出码、超时/取消状态和脱敏摘要，不给无关领域错误添加进程字段。资源释放及关键副作用可见，写入、事务、重试、取消和失败状态显式；不在构造函数、getter、静态初始化或隐式 import 隐藏关键副作用。

日志、错误、事件和测试证据均脱敏；仅记录获准的业务摘要并限长，不记录原始参数、SQL 参数或结果集。异常类型、完整堆栈、cause、suppressed 链脱敏后保留，不能因摘要限长而整体截断；完整异常链不是记录秘密的授权。

功能与 bugfix 覆盖主路径、失败路径和至少一个回归路径。依次验证目标包、直接调用者、全仓 `mvnw.cmd verify`（含真实 JAR 测试）；不适用层说明原因，未运行不写为通过。构建和隔离缓存细节读 local-dev。

MySQL 是显式环境验收：仅用已批准专用库，URL 限定回环，远程经核实目标的 SSH 隧道。普通 verify 未配置时明确跳过；启用 `-Pmysql-it` 后缺配置必须失败。未完成真实 MySQL 验证列为未覆盖，不等于框架验证失败，也不得声称持久化验收完成。

## 文档与交接

- 包职责、模块或依赖方向变化：同步 docs/architecture/、docs/code-map/。
- 用户创建公共接口后建立或更新对应包公开契约，同步 docs/contracts/module-ports.md；契约变更有明确指令，说明调用方、兼容及迁移影响。
- 源码新增、移动、删除：更新 docs/code-map/files.md；职责或验证入口变化更新 docs/code-map/modules.md。
- 构建、环境、验证命令变化更新 docs/operations/；长期技术决定记 ADR。根 REQUIREMENTS 的当前范围、验收、待确认项与事实一致；未决定内容写“待用户确认”。

主 Agent 按 Git 流程建立最终候选快照，交审核技能进行 Result Review 后核验并提交推送。高风险或跨职责报告包含：结果、影响范围、用户决策依据、验证命令与结果、两阶段审核、风险、未覆盖、偏离、commit/branch/remote/upstream/push 状态。普通交接可简短，仍报告验证、审核、commit、push、剩余风险；未执行项说明原因。

## 适用时采用的实践

将验收对应到可观察行为和验证方式；bugfix 尽可能先复现再用回归测试证明修复，无法复现说明证据与限制；测试关注行为、相互独立且可重复，避免照抄实现或任意等待。这些不是新增强制门禁，不引入覆盖率指标、设计模式或工具要求，也不授权改变业务校验、重试、事务或接口语义。
