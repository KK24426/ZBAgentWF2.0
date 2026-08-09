<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：提供 ZBAgentWF2.0 精简文档导航。
 -->

# 文档中心

本目录只保留日常开发真正需要的入口，避免让文档阅读本身成为开发负担。

## 默认阅读路径

1. 新任务先读根目录 `AGENTS.md`。
2. 按 [operations/task-entry-points.md](./operations/task-entry-points.md) 路由任务资料。
3. 常规 AI 实现读 [AI_DEV_GUIDE.md](./AI_DEV_GUIDE.md)。
4. 定位模块读 [code-map/modules.md](./code-map/modules.md)、目标模块 `AGENTS.md` 和 `REQUIREMENTS.md`。
5. 涉及架构或依赖方向时读 [architecture/module-boundaries.md](./architecture/module-boundaries.md)。
6. 涉及公共接口时先取得用户批准，再更新 [contracts/module-ports.md](./contracts/module-ports.md)。

## 文档分区

- `architecture/`：当前架构事实和依赖边界。
- `contracts/`：用户已经批准的公开端口索引和待确认事项。
- `code-map/`：模块及文件定位，不复制源码。
- `decisions/`：长期技术决策及其取舍。
- `operations/`：本地开发、AI 协作、质量和敏感资产规则。
- `templates/review/`：所有程序修改的实施前后 review 模板。

真实代码和 Maven 配置是最终依据。文档与代码不一致时必须报告并修正，不得把待确认内容写成当前事实。
