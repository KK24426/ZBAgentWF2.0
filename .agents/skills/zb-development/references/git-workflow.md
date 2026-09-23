# 主 Agent 的 Git 操作

仅授权写任务读取；只读任务不执行。权限和停止条件以根 AGENTS 为准。Git 操作前说明目标 remote、当前 branch 和 upstream，遵守 [资产规则](../../../../docs/operations/asset-policy.md)。

## 用户 checkpoint（Plan Review 前）

1. 理解用户修改并确认归属、remote/upstream 与远端状态；无修改不制造空提交。
2. 检查禁止资产、常见密钥形态、敏感信息；只显式 stage 已识别的用户文件，禁止宽泛暂存夹带无关修改。
3. 对最终 staged snapshot 再查敏感信息、禁止资产及范围。
4. 默认以 `chore: checkpoint user changes` 独立 commit，push 当前分支 upstream；核对远端 commit，工作区干净后才进入 Plan Review。

checkpoint 忠实保存用户状态，不表示构建绿色；接口调整导致的预期失败可记录命令、阶段和原因后保存。敏感信息、范围不清、远端冲突或需要 force/merge/rebase 等阻断时不得提交推送。AI 实现必须另一个 commit。

## AI 候选快照与提交

1. 完成验证后，只显式 stage 本轮完整 AI 修改；候选暂存不是提交，不绕过 checkpoint。
2. 按 [快照规则](../../zb-review/references/result-snapshot.md) 检查、记录并送 Result Review；主 Agent 提供 base/checkpoint SHA、文件清单、hash、status、验证和风险证据。
3. 审核全部通过后、commit 前重新核对 hash、清单和 status；任何差异或 unstaged/untracked 变化使原审核失效，重新验证并送审。
4. commit 仅含已审核候选；commit 后、push 前比较提交内容与已审快照，防止 hook 或外部工具改动。不一致则停止。
5. push 当前分支 upstream，核对本地和远端 commit 一致且工作区干净，按开发技能交接。

敏感/禁止/范围不明资产、验证失败、审核未通过、远端或 upstream 问题、无法隔离用户修改、提交与快照不一致均停止；仅已记录的用户 checkpoint 预期构建失败适用上述例外。不得自动 PR、发布、部署、force push、改写历史、merge 或 rebase。
