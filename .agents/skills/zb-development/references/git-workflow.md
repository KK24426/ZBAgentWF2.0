# 主 Agent 的 Git 操作

仅授权写任务读取；只读任务不执行。权限和停止条件以根 AGENTS 为准。Git 操作前说明目标 remote、当前 branch 和 upstream，遵守 [资产规则](../../../../docs/operations/asset-policy.md)。

## 开发前同步云端

1. 先检查完整 status、两类 diff、未跟踪文件及当前 branch/HEAD/upstream；确认没有 merge、rebase、cherry-pick 等进行中的 Git 操作。保留用户文件，不猜测缺失的 upstream，也不自动切换分支。
2. fetch 已核实 upstream 对应的 remote 和分支，解析并记录远端 SHA；用 `git rev-list --left-right --count HEAD...<upstream-SHA>` 核对领先/落后。网络或认证失败时停止，不在未经确认的旧代码上开始写任务。
3. 有领先提交或分叉时停止，报告未核实历史，不自动推送这些提交。有用户修改且落后远端时停止，不自动 checkpoint、stash、reset 或合并。HEAD 与 upstream 一致时，无需同步，存在用户修改则进入下面的 checkpoint 流程。
4. 仅工作区干净、无进行中操作、领先数为 0 且落后数大于 0 时，执行 `git -c merge.autoStash=false merge --ff-only --no-overwrite-ignore <已核对的upstream-SHA>`。此操作不创建 merge commit；禁止覆盖未跟踪或忽略的本地文件。Git 拒绝更新时停止并保留文件，不清理阻碍文件来强行同步。
5. 确认 HEAD 等于已核对 SHA 且工作区仍干净，重读更新后的 AGENTS、REQUIREMENTS、skills、目标规则及源码。记录同步基准；存在 checkpoint 时，推送核实后的 checkpoint SHA 成为后续评审与提交的预期远端基准。

## 用户 checkpoint（Plan Review 前）

1. 理解用户修改并确认归属、remote/upstream 与远端状态；无修改不制造空提交。
2. 检查禁止资产、常见密钥形态、敏感信息；只显式 stage 已识别的用户文件，禁止宽泛暂存夹带无关修改。
3. 对最终 staged snapshot 再查敏感信息、禁止资产及范围。
4. 默认以 `chore: checkpoint user changes` 独立 commit，push 当前分支 upstream；核对远端 commit，工作区干净后才进入 Plan Review。

checkpoint 忠实保存用户状态，不表示构建绿色；接口调整导致的预期失败可记录命令、阶段和原因后保存。仅在 HEAD 与已 fetch 的 upstream 一致时创建；提交前再次 fetch 核对，远端前进则停止。敏感信息、范围不清、远端冲突或需要 force、合并分叉、创建 merge commit、rebase 等阻断时不得提交推送。AI 实现必须另一个 commit。

## AI 候选快照与提交

1. 完成验证后，只显式 stage 本轮完整 AI 修改；候选暂存不是提交，不绕过 checkpoint。
2. 按 [快照规则](../../zb-review/references/result-snapshot.md) 检查、记录并送 Result Review；主 Agent 提供 base/checkpoint SHA、文件清单、hash、status、验证和风险证据。
3. 审核全部通过后、commit 前再次 fetch，核对 upstream SHA 仍等于同步或 checkpoint 基准；远端前进时停止，重新评估并评审，不将新提交直接混入已审快照。重新核对 hash、清单和 status；任何差异或 unstaged/untracked 变化使原审核失效，重新验证并送审。
4. commit 仅含已审核候选；commit 后、push 前比较提交内容与已审快照，防止 hook 或外部工具改动。不一致则停止。
5. 普通 push 当前分支 upstream；被拒绝则停止，不强推或自动重试合并。核对云端 HEAD 与远端分支 commit 一致且工作区干净，再尝试可选本地同步，按开发技能交接。

## 可选本地同步

云端推送成功且已核实后，只有存在实际可访问的本地执行器时才执行；项目列表中的本地路径不代表执行权限或连接。先核实本地仓库 remote、分支及 upstream 与本次推送目标一致，再检查完整 status、进行中的 Git 操作并 fetch。仅工作区干净、无进行中操作、无领先或分叉时，以开发前同步的受保护快进命令更新到核对后的 upstream SHA，并确认 HEAD 与远端一致。不要切换本地分支、checkpoint 或推送本地修改来完成同步。

本地不可访问、用户未提交修改、领先、分叉、认证失败或文件保护阻断时保留本地状态，报告“未同步”及原因，并提供 [手动更新步骤](../../../../docs/operations/local-dev.md#本地与-codex-云端协作)。本地同步失败不撤回已成功的云端提交；交接区分云端 push 与本地同步状态。不新增后台同步程序，也不通过 push 自动启动 Codex 任务。

敏感/禁止/范围不明资产、验证失败、审核未通过、远端或 upstream 问题、无法隔离用户修改、提交与快照不一致均停止；仅已记录的用户 checkpoint 预期构建失败适用上述例外。允许上述受保护的 fast-forward；不得自动 PR、发布、部署、force push、改写历史、创建 merge commit、合并分叉或 rebase。
