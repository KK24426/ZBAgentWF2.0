# Plan Review Packet

本文件是空白模板，不填写具体任务结果。Plan Review 通过前在任务对话中提供以下内容；通过后整理到 `tmp/reviews/<task-id>/plan-review.md`，保留各轮记录。保存与追溯规则见 [Agent Workflow](../../operations/agent-workflow.md)。

## Review 元数据

- Review Type：Plan Review
- Round：1 / 2 / 3
- Base/Checkpoint SHA：
- 指定 Reviewer 集合：
- 实施者：
- Review 范围：

## 用户原始需求

保留用户原始表达或可追溯摘录。

## 用户已批准的设计

列出包职责、接口、状态机、协议、技术选择和 Git 自动化的明确批准记录；涉及 Maven 模块结构时单独说明。

## 实施方案

- 修改范围：
- 不涉及范围：
- 预计文件：
- 验证计划：目标包 / 直接调用者 / 全仓
- 数据、安全、环境和发布影响：

## Findings

### P0

- 无 / 发现：

### P1

- 无 / 发现：

### P2

- 无 / 发现：

### P3

- 无 / 发现：

P0/P1 阻塞；P2/P3 默认不阻塞，除非 reviewer 明确说明真实阻塞原因。

## Reviewer 结论

| Reviewer Identity | Round | Acceptance | P0 | P1 | P2 | P3 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  | Accept / Revise |  |  |  |  |  |

## 汇总门禁

- 所有指定 reviewer 均已返回：Yes / No
- 所有指定 reviewer 均为 Accept：Yes / No
- Acceptance：Accept / Revise
- Can Implement：Yes / No
- 未解决冲突或交用户裁决事项：

Plan Review 最多三轮。只有所有指定 reviewer 都 `Accept` 且 `Can Implement: Yes` 时才能开始修改项目文件。
