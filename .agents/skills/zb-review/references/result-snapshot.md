# 最终候选快照核验

用于 Result Review，由主 Agent 建立候选，独立 reviewer 只读审核。

1. 核验 base/checkpoint SHA、最终 staged 文件清单、完整 staged diff、`git status` 和验证证据；执行 `git diff --cached --check`。
2. 检查敏感信息、[禁止资产](../../../../docs/operations/asset-policy.md)、非目标二进制、文件范围，以及契约、文档、构建、code map 一致性。忽略文件的实现修改单独核验。
3. 记录 staged diff hash，使用：

   ```powershell
   git diff --cached --full-index --binary | git hash-object --stdin
   ```

4. 核验目标包、直接调用者、全仓验证与任务验收；未执行项及原因必须可见，普通 verify 不能代替真实 MySQL 验收。检查用户修改保护、副作用目标、异常及日志脱敏、资源释放、回归和兼容风险。
5. 对此快照返回各级发现及真实结论，记录到本地 result-review，不修改候选。全部指定 reviewer Accept 且 Can Commit/Push: Yes 才允许主 Agent 进入提交前复核。

commit 前 hash、清单或 status 有任何差异，含 unstaged/untracked 变化，原审核立即失效，必须重新验证评审。commit 后、push 前内容必须与被审快照一致；主 Agent 按开发技能的 Git 流程执行并检查，不能把本次结论用于其它快照。
