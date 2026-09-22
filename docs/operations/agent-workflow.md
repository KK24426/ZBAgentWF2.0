<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：定义用户修改 checkpoint、AI 实现、双阶段评审和自动提交推送流程。
 -->

# Agent Workflow

本文是日常协作流程的详细权威。用户所有权、AI 修改权限、停止条件和规则优先级仍以根目录及目标包的 `AGENTS.md` 为准；本文不得放宽这些门禁。

## 标准流程

```text
读取用户修改
  -> 用户修改 checkpoint commit/push（存在时）
  -> 读取规则、接口、REQUIREMENTS 和契约
  -> Plan Review
  -> AI 实现与分层验证
  -> 建立并检查最终 staged snapshot
  -> Result Review
  -> AI commit/push
  -> 核对本地、commit、远端和干净工作区
```

只读问答、计划和 review-only 任务不进入写入、checkpoint、commit 或 push 流程。

## 1. 写入前读取用户修改

每个 AI 写任务开始时必须检查：

- 当前 branch、upstream、HEAD 与远端状态；
- `git status` 中的 unstaged、staged、untracked、rename 和 delete；
- unstaged diff 与 staged diff；
- 未跟踪文件内容，以及用户修改涉及的接口、DTO、状态、错误语义、构建、配置和契约文档。

AI 应先形成影响摘要，再决定实现范围。不得格式化、回退、覆盖或夹带修改用户文件；范围或归属无法安全区分时停止并请用户确认。

## 2. 用户修改 checkpoint

存在用户修改时，按以下顺序独立保存：

1. 确认 remote、当前 branch 和 upstream，不得自动 merge、rebase、改写历史或 force push。
2. 检查禁止资产、常见密钥形态和敏感信息。
3. 只显式 stage 已识别的用户修改；不得使用可能夹带无关内容的宽泛暂存方式。
4. 以最终 staged snapshot 再次执行敏感信息、禁止资产和文件范围检查。
5. 默认使用 `chore: checkpoint user changes` 创建独立 commit，并 push 当前分支 upstream。
6. 核对远端 commit 和工作区；只有工作区恢复干净后才能进入 Plan Review。

checkpoint 忠实保存用户状态，不代表构建绿色。用户正在调整接口而导致预期构建失败时，可以记录失败命令、阶段和原因后 checkpoint；密钥、范围不清、远端冲突、需要 force/merge/rebase 或其它安全阻断时不得提交或推送。没有用户修改时不得创建空 checkpoint。

## 3. Plan Review

所有程序修改都必须在写入前完成 Plan Review，包括：

- Java 源码；
- POM、构建、CI 和脚本；
- 配置、schema 和 migration；
- 公开接口、DTO、状态、错误语义和其它契约；
- 工程流程、质量门禁、权限规则和模块边界。

Plan Review 以用户 checkpoint commit、当前源码、就近规则、根 `REQUIREMENTS.md` 和相关契约为事实输入。模板见 `docs/templates/review/PLAN_REVIEW.md`。只有汇总结论为 `Acceptance: Accept` 且 `Can Implement: Yes` 时才能写项目文件。

## 4. 实现与分层验证

AI 只实现用户已定义或批准边界后的具体功能。公开契约以当前真实 Java 源码和测试为最终事实；变更公开契约时同步公开出口、`docs/contracts/module-ports.md` 和相关文档。

bugfix 必须先检查报错点、同模块调用点、直接调用方、共享状态、错误映射和回归路径，不能只修表面报错。

验证顺序为：

1. 目标包测试；
2. 直接调用者测试；
3. 仓库级 `mvnw.cmd verify`（包括真实 JAR 测试）。

`mysql-it` 为显式环境验收：只使用用户提供的本机专用测试库。未配置时普通 verify 明确跳过 MySQL IT；启用 `-Pmysql-it` 后缺配置必须失败。未完成真实 MySQL 验证必须在交接中列为未覆盖项，不等于框架代码验证失败，也不能声称持久化已验收。

如果某一层不适用，应记录原因，不得把“未运行”写成“通过”。

## 5. Result Review 与最终提交快照

实现和验证完成后：

1. 只显式 stage 本轮完整 AI 修改，建立最终提交候选快照。
2. 对 staged snapshot 执行 `git diff --cached --check`、敏感信息、禁止资产、非目标二进制、文件范围、契约、文档、构建和 code map 一致性检查。
3. 记录 base/checkpoint SHA、staged 文件清单、`git status`，并使用下列只读命令计算 staged diff hash：

   ```powershell
   git diff --cached --full-index --binary | git hash-object --stdin
   ```

4. Result Review 必须直接审查该 staged snapshot，并在 `docs/templates/review/RESULT_REVIEW.md` 记录 hash。只有汇总结论为 `Acceptance: Accept` 且 `Can Commit/Push: Yes` 时才能提交。
5. commit 前重新核对 hash、文件清单和 `git status`。存在 unstaged/untracked 变化或任何差异时，原 Result Review 立即失效，必须重新验证和评审。
6. commit 只包含已评审 staged snapshot。commit 后、push 前比较 commit 内容与已评审快照，防止 hook 或外部工具改变内容；不一致时停止。
7. push 当前分支 upstream，随后核对本地与远端 commit，并确认工作区干净。

Result Review 前的 stage 只是建立可审查候选快照，不等于 commit/push，也不绕过用户修改 checkpoint。

## 6. Reviewer 与轮次

- 默认至少一个真实、独立的 reviewer；不得由实施者伪造独立结论。
- 可以指定多个 reviewer。review packet 必须记录指定集合、每位 reviewer 的身份、轮次和结论；只有所有指定 reviewer 都 `Accept`，汇总门禁才通过。
- P0/P1 阻塞；P2/P3 默认不阻塞，除非 reviewer 明确说明其构成真实阻塞的原因。
- Plan Review 和 Result Review 各自最多三轮。reviewer 不可用、结论冲突，或三轮后仍有 P0/P1 时，停止并交给用户裁决。

## 7. 提交与推送停止条件

本仓库已获用户授权按上述流程自动 commit/push。以下任一情况必须停止：

- staged snapshot 含敏感信息、禁止资产或范围不明文件；
- 验证失败且不是已记录的用户 checkpoint 预期失败；
- Plan Review 或 Result Review 门禁未通过；
- remote/upstream 不明确、远端存在冲突或需要 force、merge、rebase；
- 存在无法与本轮安全隔离的用户修改；
- commit 内容与已评审 snapshot 不一致。

不得自动创建 PR、发布或部署，除非用户另行明确要求。
