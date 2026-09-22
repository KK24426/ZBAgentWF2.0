<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：汇总实现、契约、评审、验证和发布的轻量质量门禁。
 -->

# Quality Gates

详细执行顺序和 staged snapshot 规则以 [Agent Workflow](./agent-workflow.md) 为准。

## 所有程序修改

- 已读取并独立 checkpoint 用户修改（如存在）；
- 已按 [Task Entry Points](./task-entry-points.md) 读取就近规则和相关事实；
- 修改范围、不涉及范围和验收条件明确；
- Plan Review 汇总 `Accept` 且 `Can Implement: Yes`；
- 未擅自修改用户拥有的公共边界；
- 文档与当前代码事实一致。

## 功能实现与 bugfix

- 接口和验收条件已经由用户定义或批准；
- 覆盖主路径、失败路径和至少一个回归路径；
- bugfix 已检查同模块调用点、直接调用方和共享状态；
- adapter 不复制领域规则；
- 适用时保留错误来源、模块、阶段、业务 ID、退出码、超时/取消和脱敏摘要；
- 资源释放和关键副作用可见。

## 契约和文档

- 公开契约变更有用户明确指令；
- 说明调用方、兼容性和迁移影响；
- 同步公开出口、契约索引、架构和 code map；
- 新增、移动、删除源码后更新 `docs/code-map/files.md`；
- 根 `REQUIREMENTS.md` 的当前范围、验收和待确认事项与事实一致。

## 数据和外部副作用

- 环境、数据库和目标配置已经明确；
- 写入、事务、重试、取消和失败状态显式；
- 不在构造函数、getter、静态初始化或隐式 import 中隐藏关键副作用；
- 日志、错误、事件和测试证据已脱敏并限制长度。

## 验证与 Result Review

- 按目标包、直接调用者、全仓分层验证；
- 最终 staged snapshot 通过 `git diff --cached --check`、敏感信息、禁止资产和非目标二进制检查；
- review packet 记录 base/checkpoint SHA、staged 文件、staged diff hash 和指定 reviewer 集合；
- 所有指定 reviewer `Accept`，汇总 `Can Commit/Push: Yes`；
- commit 前 hash、文件清单和工作区状态与评审输入完全一致；
- commit 后确认 commit 内容与已评审 snapshot 等价。

## 提交与推送

- remote、branch 和 upstream 明确；
- 不需要 force、merge、rebase 或改写历史；
- commit 仅包含已评审 staged snapshot；
- 推送后本地与远端 commit 一致，工作区干净。

## 最终报告

高风险或跨模块修改使用完整报告：结果、影响范围、用户决策依据、验证命令与结果、Plan/Result Review、风险、未覆盖事项、偏离记录、commit、branch、remote/upstream 和 push 状态。

普通修改可以简洁报告，但仍须包含验证、评审、commit、push 和剩余风险。未执行项必须说明原因。
