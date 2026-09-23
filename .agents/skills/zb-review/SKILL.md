---
name: zb-review
description: 审查 ZBAgentWF2.0 的实现计划、最终 staged snapshot 或执行独立只读代码审查时使用；输出问题与门禁结论，不代替开发或修改被审文件。
---

# 项目独立审核

遵守根 [AGENTS.md](../../../AGENTS.md) 和被审目录就近规则。审核者独立于实施者；不得以切换 skill 冒充独立 reviewer。只读检查，不修被审文件、不 stage/commit/push；修正交回开发环节。

## 输入与审核模式

读取用户原始需求、批准记录、根 REQUIREMENTS、相关契约、真实源码/测试、基准 SHA 和范围；仅选对应阶段材料。检查实现规范时按需读取 [开发技能](../zb-development/SKILL.md) 的实现、验证及文档要求，不执行其写入流程。

- **Plan Review**：使用 [计划模板](assets/PLAN_REVIEW.md)。基于当前源码和用户 checkpoint（无用户修改则当前基准）核验授权、用户修改理解、范围、接口兼容、方案、验收、分层验证、数据及环境影响。Plan 通过前仅在任务对话提交 packet 与独立结论，不为记录提前写项目文件。
- **Result Review**：使用 [结果模板](assets/RESULT_REVIEW.md) 和 [快照规则](references/result-snapshot.md)。直接审最终 staged snapshot、调用方及证据，不能只信开发摘要；核验与批准方案的偏离、代码/契约/文档/构建/code map/需求一致性以及未覆盖风险。
- **只读审查**：明确用户指定的分支、diff 或源码范围，报告事实、问题和未核验项；不要求建立提交候选，也不签发该快照的提交许可。

## 结论与记录

packet 记录指定 reviewer 集合、每人真实身份、轮次、输入和结论。全部指定 reviewer 返回且全部 Accept 才通过；Plan 需 `Can Implement: Yes`，Result 需 `Can Commit/Push: Yes`。问题分级、两阶段各最多三轮及停止条件遵守根规则；存在阻断不得写通过。

适用时，发现写清位置、触发条件、影响、依据，区分已确认缺陷、待验证风险与风格建议。此补充实践不构成新的格式门禁；P2/P3 只有明确真实阻塞原因才阻塞，不能以偏好扩大修改范围。

Plan 通过后，由主 Agent 将计划及真实 reviewer 结论保存到 `tmp/reviews/<task-id>/plan-review.md`；Result 使用同目录 `result-review.md`，task-id 用日期加任务短名。保留每轮输入、发现、修正和实际结论，追加不覆盖；Result 每轮记录 hash、文件清单和验证证据。

assets 是空白模板，不填任务结果。上述 tmp 记录被 Git 忽略，不暂存或强制加入提交，也不提供 Git 持久追溯；最终交接仍报告轮次、结论及提交状态。hash 和结论不得写回已审 staged 文件，避免改变快照。忽略路径不是权限豁免：实现若修改本地配置或其它忽略文件，也须列入审核并单独核验。
