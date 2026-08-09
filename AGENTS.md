<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义 ZBAgentWF2.0 中用户主导架构、AI 受控实现的仓库级协作规则。
 -->

# AGENTS.md

## 仓库定位

ZBAgentWF2.0 是一个由用户亲自掌握架构和公共接口、由 AI 在已批准边界内实现具体功能的 Java 项目。当前仓库只包含 Java 26、Maven 和四个空模块的最小骨架，不代表任何业务能力已经实现。

默认使用中文沟通、文档和注释；Java 标识符、命令、协议字段和第三方名称保留原语言。子目录存在 `AGENTS.md` 时，应同时遵守根规则和就近规则；就近规则只能补充或收紧约束，不得放宽、覆盖或绕过本文件的用户决策权硬门禁。规则冲突时以更严格者为准，无法判断时停止并请用户裁决。

## 用户决策权硬门禁

除非用户在当前任务中明确要求或逐项批准，AI 不得创建、删除或修改：

- Maven 模块及模块职责；
- 公共 Java 接口、`public` DTO 和公开异常语义；
- 领域状态机、核心业务规则和事务边界；
- 跨进程协议、持久化 schema、配置契约和权限模型；
- 模块依赖方向和公开入口；
- 任意 `AGENTS.md`，以及定义用户所有权、AI 修改权限或规则优先级的治理内容。

如果实现过程中发现现有接口不足，AI 必须在写入相关文件前停止，并向用户提交“接口不足、建议变更、影响范围、兼容性风险和备选方案”。不得为了让实现通过而先改接口、再在结果中补报。

用户明确授权 AI 编写某个框架或接口时，该授权只覆盖指定范围，不自动扩展到其它模块或契约。

本文是用户所有权与 AI 修改权限的唯一规范权威。`docs/AI_DEV_GUIDE.md` 和 `docs/operations/agent-workflow.md` 只提供使用入口与流程说明；内容不一致时以本文为准，且不得据此放宽本文门禁。

## AI 默认可执行范围

在用户已经提供或批准的框架、接口和验收条件内，AI 可以：

- 实现包内类和接口实现；
- 实现 adapter、序列化、数据库访问、外部调用和组合代码；
- 编写单元测试、集成测试、测试 fixture 和必要文档；
- 修复明确 bug，并检查同模块调用点和直接调用方；
- 运行构建、测试、静态检查和只读诊断。

实现仍应小步、可验证。不要顺手引入 Spring、数据库、消息队列、Agent SDK、桌面框架或其它基础设施。

## 当前模块和依赖方向

- `modules/domain`：用户主导的领域对象、业务规则和状态机；不得依赖其它项目模块或 I/O 实现。
- `modules/application`：用户主导的用例和出站端口；只允许依赖 `domain`。
- `modules/adapters`：AI 可实现外部能力 adapter；允许依赖 `application` 和 `domain`。
- `apps/runtime-host`：未来进程入口和组合根；允许依赖 `adapters`、`application` 和 `domain`。

当前 `package-info.java` 只标记包边界，不是业务契约，也不证明对应运行能力已存在。

## 开发流程

1. 先按 `docs/operations/task-entry-points.md` 读取根规则、就近规则、模块需求台账和任务相关文档。
2. 在任何 AI 写入前检查并理解用户修改；详细 checkpoint、评审、验证和提交顺序以 `docs/operations/agent-workflow.md` 为准。
3. 确认用户是否已经定义接口、验收条件和允许修改范围，说明修改范围、不涉及范围和验证计划。
4. 完成 Plan Review；门禁未通过时不得修改项目文件。
5. 只实现已批准边界后的具体功能，按“目标模块、直接调用者、全仓”分层验证。
6. 建立最终 staged snapshot，完成 Result Review；门禁未通过时不得 commit 或 push。
7. 提交并推送已评审快照，说明实际修改、验证结果、风险、未覆盖事项和 Git 状态。

所有程序修改都必须进行 Plan Review 和 Result Review，包括 Java 源码、POM、构建/CI/脚本、配置、schema、migration、公开契约，以及工程流程、质量门禁和模块边界。Plan Review 与 Result Review 各自最多三轮，评审定义和阻断语义以 `docs/operations/agent-workflow.md` 为准。

## 用户修改优先与版本追溯

- 每个 AI 写任务开始时，必须读取 unstaged、staged、untracked、rename 和 delete 状态，重点理解用户对接口、DTO、状态、错误语义、构建和契约文档的修改。
- 不得格式化、回退、覆盖或夹带修改用户文件。无法安全区分归属时停止并请用户确认。
- 存在用户修改时，先按 `docs/operations/agent-workflow.md` 完成敏感信息检查、独立 checkpoint commit 和 push；没有用户修改时不得制造空提交。
- AI 修改与用户 checkpoint 必须使用不同 commit。AI 修改仅在验证和 Result Review 通过后提交、推送。

## 环境和关键副作用

- 当前不预设数据库、远程环境、生产环境或真实 Agent provider。
- migration、写库、远程调用、真实 Agent 执行、文件写入、进程启动、commit、push 和部署必须显式说明目标。
- 不得把 token、API key、password、证书、私钥、账号或隐私材料写入仓库、日志、测试快照或文档。
- 外部进程必须使用结构化 `command + args + stdin`，不得暴露任意 shell 字符串入口。
- 本仓库已获用户授权：满足 `docs/operations/agent-workflow.md` 的 checkpoint 或已评审实现条件时，自动 commit 并 push 当前分支 upstream；不得自动创建 PR、force push、改写历史、merge 或 rebase。

## 代码和注释

新增 Java 文件使用以下文件头，并补充准确职责：

```java
/*
 * 创建日期：
 * 更新日期：
 * 做 成 者：zebiao
 * 版    本：
 * 功能概要：
 */
```

公共类型、关键分支、状态机、安全判断和副作用边界需要中文说明；简单代码不堆砌低价值注释。通用异常处理、网络、文件和数据访问能力应保留扩展空间；只有出现第二个真实消费者或用户明确批准后才提取共享能力，新增 `common` 模块仍须用户批准。

错误和异常不得被吞掉。适用时保留来源、模块、阶段、业务 ID、退出码、超时/取消状态和可脱敏摘要；不要求与进程无关的领域错误携带无关字段。

## 文档同步

- 模块或依赖方向变化：更新 `docs/architecture/` 和 `docs/code-map/`。
- 用户创建公共接口后：在目标模块建立或更新公开契约文档，并同步 `docs/contracts/module-ports.md`。
- 新增、移动或删除源码文件：同步更新 `docs/code-map/files.md`；模块职责或验证入口变化时同步更新 `docs/code-map/modules.md`。
- 构建、环境或验证命令变化：更新 `docs/operations/`。
- 长期技术决定：记录 ADR。
- 未决定内容必须写成“待用户确认”，不得写成当前事实。

## 结果交接

高风险或跨模块修改使用完整最终报告，至少包含结果、影响范围、用户决策依据、验证命令与结果、Plan/Result Review、风险、未覆盖事项、偏离记录，以及 commit/push 状态。普通修改可以简洁交接，但仍须报告验证、评审、commit、push 和剩余风险。
