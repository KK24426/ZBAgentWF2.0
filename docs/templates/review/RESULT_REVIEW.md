# Result Review Packet

## Review 元数据

- Review Type：Result Review
- Round：1 / 2 / 3
- Base/Checkpoint SHA：
- Staged Diff Hash：
- Staged 文件清单：
- 指定 Reviewer 集合：
- 实施者：
- Review 范围：

## 对应需求和用户决策

记录原始需求、批准的公共边界和已通过 Plan Review 的方案。

## 实际改动

- 实现行为：
- 与计划偏差：
- 未修改范围：

## 验证与快照检查

- 目标模块验证：
- 直接调用者验证：
- 全仓验证：
- `git diff --cached --check`：
- 敏感信息和禁止资产：
- 非目标二进制：
- 契约、文档、构建和 code map 一致性：
- 未运行项及原因：

## 风险

- 兼容性：
- 数据和安全：
- 剩余风险：
- 未覆盖事项：

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
- Can Commit/Push：Yes / No
- commit 前快照复核结果：
- 未解决冲突或交用户裁决事项：

Result Review 最多三轮。只有所有指定 reviewer 都 `Accept`、`Can Commit/Push: Yes`，且 commit 前 staged diff hash 和文件清单保持不变时才能提交。
