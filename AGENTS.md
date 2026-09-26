<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.7
 * 功能概要：定义 ZBAgentWF2.0 中用户主导架构、AI 受控实现的仓库级协作规则。
 -->

# AGENTS.md

## 仓库定位

ZBAgentWF2.0 是一个由用户亲自掌握架构和公共接口、由 AI 在已批准边界内实现具体功能的 Java 项目。当前为 Java 26 单 Maven 工程，按 user、agent、common 三个包协作；已接入 Spring Boot Web、简单首页、日志和可选 MyBatis/MySQL 基础，不代表业务持久化已经实现。CLI 已移除。

默认使用中文沟通、文档和注释；Java 标识符、命令、协议字段和第三方名称保留原语言。子目录存在 `AGENTS.md` 时，应同时遵守根规则和就近规则；就近规则只能补充或收紧约束，不得放宽、覆盖或绕过本文件的用户决策权硬门禁。规则冲突时以更严格者为准，无法判断时停止并请用户裁决。

## 用户决策权硬门禁

除非用户在当前任务中明确要求或逐项批准，AI 不得创建、删除或修改：

- Maven 模块结构及包的职责范围；
- 用户定义的业务接口、DTO 契约和公开异常语义；
- 领域状态机、核心业务规则和事务边界；
- 跨进程协议、持久化 schema、配置契约和权限模型；
- 包或 Maven 模块的依赖方向和公开入口；
- 任意 `AGENTS.md`，以及定义用户所有权、AI 修改权限或规则优先级的治理内容。

如果实现过程中发现现有接口不足，AI 必须在写入相关文件前停止，并向用户提交“接口不足、建议变更、影响范围、兼容性风险和备选方案”。不得为了让实现通过而先改接口、再在结果中补报。

用户明确授权 AI 编写某个框架或接口时，该授权只覆盖指定范围，不自动扩展到其它包、职责范围或契约。

用户已授权：Agent 可以在具体任务范围内编写技术性 Mapper 接口和 SQL，并补充、调整 common 中的 Bean 与工具；涉及用户业务接口、字段语义、schema 或事务边界变化时，必须先说明影响并取得确认。不得借通用类调整绕过业务边界。

用户已授权：Agent 的具体实现以 user 包最新的接口和抽象类定义为准。用户调整方法名称、签名或职责后，AI 应同步修改具体实现、直接调用方、测试和契约文档；因该接口变化而失效、无法适配新契约的旧实现方法或旧兼容入口应直接删除，无需再逐项确认。不得用空实现、默认成功或仅抛“未支持”的兼容壳保留这些旧方法。删除范围仅限本次接口变化的直接影响；不得因实现困难删除 user 包中仍有效的方法，也不授权扩大业务规则或其它公共契约。真正的接口不足仍按前述不足处理流程提交用户决定。

本文是用户所有权与 AI 修改权限的唯一规范权威。项目 skills 只提供执行方法与流程说明；内容不一致时以本文为准，且不得据此放宽本文门禁。

## AI 默认可执行范围

在用户已经提供或批准的框架、接口和验收条件内，AI 可以：

- 实现包内类和接口实现；
- 实现 adapter、序列化、数据库访问、外部调用和组合代码；
- 编写单元测试、集成测试、测试 fixture 和必要文档；
- 修复明确 bug，并检查同包调用点和直接调用方；
- 运行构建、测试、静态检查和只读诊断。

实现仍应小步、可验证。当前已批准 Spring Boot Web 简单首页、MyBatis/MySQL 和日志；不要顺手引入业务 Web API、消息队列、Agent SDK、桌面框架或其它基础设施。

## 当前包和职责

这是一个 Maven JAR 工程，不使用 Maven 子模块或额外依赖隔离。

- 根包 `com.kk24426.zbagentwf`：Web 服务启动、初始化和组合入口。
- `user`：用户定义接口、编排和手写业务代码，AI 按明确任务修改。
- `agent`：Agent 实现已批准接口，以及技术性的 Mapper 和 SQL。
- `common`：双方共同维护 Bean、DTO 和工具；按上面的用户决策权边界调整。
- Spring 负责组件扫描与接口注入。Mapper 位于 `agent.persistence.mapper`，XML 位于资源目录 `mapper/`。
- 不预建业务接口、业务表或通用 CRUD 基类。生产 schema 和事务边界仍由用户决定。

## 任务路由与强制门禁

所有任务先读根规则、目标目录就近规则和根 `REQUIREMENTS.md`（唯一需求台账，不新建包级副本），再读任务相关源码、测试及事实文档；真实代码和 Maven 配置是最终依据。只读问答、计划和 review-only 任务不进入写入、checkpoint、提交或推送流程。

- 开发、修复、测试以及工程流程修改必须读取 [zb-development](.agents/skills/zb-development/SKILL.md)。只读计划也可读取，但不得执行写入阶段。
- Plan Review、Result Review 和独立只读审查必须读取 [zb-review](.agents/skills/zb-review/SKILL.md)。
- 资料按任务选读：架构看 architecture、code-map 和相关 ADR；契约看 contracts 与公开源码；构建看 POM、Wrapper 和 local-dev；数据及外部副作用看 asset-policy、相关契约和目标包规则。开发 skill 提供详细路由。
- skill 未自动发现时直接读取上述文件；必需文件缺失时停止，不得跳过门禁。任务边界无法确定时请用户裁决，不以新增抽象、包或模块掩盖。

主 Agent 负责协调、返工及全部 Git 写操作。顺序为：理解用户修改 → 用户 checkpoint（存在时）→ Plan Review → 实现与分层验证 → 最终 staged snapshot → Result Review → commit/push 与交接。

所有程序修改均须双阶段评审，包括 Java、POM、构建/CI/脚本、配置、schema、migration、公开契约、工程流程、质量门禁、权限规则及包职责边界。至少一个真实独立 reviewer，不得由实施者切换 skill 冒充独立审核；多个指定 reviewer 必须全部 Accept。Plan 的 `Acceptance: Accept` 且 `Can Implement: Yes` 才能修改项目文件；Result 的 `Acceptance: Accept` 且 `Can Commit/Push: Yes` 才能提交推送。

两个阶段各最多三轮，通过即结束；P0/P1 阻塞，P2/P3 默认不阻塞，除非 reviewer 说明真实阻塞原因。reviewer 不可用、结论冲突或三轮后仍有 P0/P1 时停止并交用户裁决。Result 必须审最终 staged snapshot；评审后快照、文件清单或工作区变化使原结论失效，须重新验证评审。

## 用户修改优先与版本追溯

- 每个 AI 写任务开始时，必须读取 unstaged、staged、untracked、rename 和 delete 状态，重点理解用户对接口、DTO、状态、错误语义、构建和契约文档的修改。
- 不得格式化、回退、覆盖或夹带修改用户文件。无法安全区分归属时停止并请用户确认。
- 存在用户修改时，先按 [Git 流程](.agents/skills/zb-development/references/git-workflow.md) 完成敏感信息检查、独立 checkpoint commit 和 push；没有用户修改时不得制造空提交。
- AI 修改与用户 checkpoint 必须使用不同 commit。AI 修改仅在验证和 Result Review 通过后提交、推送。

## 环境和关键副作用

- 用户已批准指定远程测试服务器上的 Web 服务，以及现有 MySQL 实例中新建的专用 zbagentwf_test 库和受限账号；不得操作同实例其它项目的数据或服务。具体地址、密钥和密码保存在仓库外，不进入公开文档。
- Web 默认仅监听 127.0.0.1，通过 SSH 隧道访问；未批准公网开放、登录权限模型或业务 API。数据库测试仍仅接受回环地址的 zbagentwf_test，可通过已验证目标的 SSH 隧道连接远程专用库；未明确授权时不得查找或提取其它凭据。
- migration、写库、远程调用、真实 Agent 执行、文件写入、进程启动、commit、push 和部署必须显式说明目标。
- 不得把 token、API key、password、证书、私钥、账号或隐私材料写入仓库、日志、测试快照或文档。
- 外部进程必须使用结构化 `command + args + stdin`，不得暴露任意 shell 字符串入口。
- 本仓库已获用户授权：满足 [Git 流程](.agents/skills/zb-development/references/git-workflow.md) 的 checkpoint 或已评审实现条件时，自动 commit 并 push 当前分支 upstream；不得自动创建 PR、force push、改写历史、merge 或 rebase。

## 执行规范与交接

编码、注释、异常、文档同步和报告要求由开发 skill 维护，审核 skill 核验；不因拆分而豁免。共享能力只在第二个真实消费者出现或用户明确批准后提取；不得借此扩展包或模块边界。

提交停止条件：敏感或禁止资产、文件范围不明、验证失败（已记录的用户 checkpoint 预期失败除外）、评审未通过、remote/upstream 不明或远端冲突、无法隔离用户修改、commit 内容偏离已审快照。不得自动创建 PR、发布或部署，除非用户另行明确要求。
