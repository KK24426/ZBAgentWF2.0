<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：索引当前 Java 源文件职责。
 -->

# File Index

| 文件 | 职责 | 公开契约 |
| --- | --- | --- |
| `modules/domain/.../package-info.java` | 标记领域包边界 | 否 |
| `modules/application/.../package-info.java` | 标记应用包边界 | 否 |
| `modules/adapters/.../package-info.java` | 标记 adapter 包边界 | 否 |
| `apps/runtime-host/.../package-info.java` | 标记组合根包边界 | 否 |

当前没有业务源文件。用户添加接口或 AI 添加实现后，同步补充实际入口和测试位置。

## 规则与需求入口

| 文件 | 职责 |
| --- | --- |
| `AGENTS.md` | 仓库级用户所有权、AI 修改权限和硬门禁 |
| `docs/operations/task-entry-points.md` | 按任务类型路由必读资料 |
| `docs/operations/agent-workflow.md` | checkpoint、双阶段评审和自动 commit/push 流程 |
| `docs/operations/quality-gates.md` | 质量门禁和最终报告要求 |
| `docs/templates/review/PLAN_REVIEW.md` | Plan Review packet |
| `docs/templates/review/RESULT_REVIEW.md` | 绑定 staged snapshot 的 Result Review packet |
| `modules/*/REQUIREMENTS.md`、`apps/runtime-host/REQUIREMENTS.md` | 就近范围、验收和待确认事项台账 |
