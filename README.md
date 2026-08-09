<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：介绍 ZBAgentWF2.0 的 Java 工程骨架、人机职责和开始开发方式。
 -->

# ZBAgentWF2.0

ZBAgentWF2.0 是 ZBAgentWF 的全新 Java 实现起点。项目采用“用户主导架构和接口，AI 实现已批准边界后的具体功能”的协作方式，使核心设计保持在用户掌握之中，同时利用 AI 完成实现、测试和重复性工程工作。

当前仓库是空业务骨架，不包含旧 ZBAgentWF 业务代码，也没有实现数据库、Agent provider、进程协议或客户端。

## 当前技术基线

- Java 26
- Apache Maven 3.9.16
- Maven Wrapper 3.3.4（only-script，不提交 Wrapper JAR）
- 模块化单体
- 默认中文文档和注释

本轮没有引入 Spring、数据库驱动、Agent SDK 或 UI 框架。这些选择由用户在真实需求出现后决定。

## 模块

```text
apps/runtime-host
  -> modules/adapters
      -> modules/application
          -> modules/domain
```

- `domain`：领域对象、规则和状态机，由用户定义。
- `application`：用例接口和出站端口，由用户定义。
- `adapters`：AI 根据批准接口实现外部系统和基础设施接入。
- `runtime-host`：未来进程入口和组合根，当前没有运行代码。

四个模块现在只有构建文件、包边界和就近协作规则。`package-info.java` 不构成业务接口。

## 人机协作方式

用户负责：

- 模块和依赖方向；
- 公共接口与 DTO；
- 状态机和核心规则；
- 协议、配置、数据库 schema 和事务边界；
- 关键技术选型与验收标准。

AI 负责：

- 已批准接口的具体实现；
- adapter、数据访问和外部调用；
- 单元测试、集成测试和回归验证；
- 实现文档和影响范围说明。

接口不足时，AI 必须先提出建议并等待用户决定，不能自行扩展公共契约。完整规则见 [AGENTS.md](./AGENTS.md) 和 [AI Development Guide](./docs/AI_DEV_GUIDE.md)。

## 开始使用

本机需要 JDK 26。Maven 由 Wrapper 自动准备：

```powershell
.\mvnw.cmd verify
```

开始第一个业务功能前，建议用户先完成：

1. 在 `domain` 中定义最小领域对象和规则。
2. 在 `application` 中定义一个真实用例接口及其所需端口。
3. 给出输入、输出、失败语义和验收示例。
4. 再让 AI 在 `adapters` 或 application 内部实现具体功能和测试。

不要一次性设计完整平台。优先选择一条可以真实验证的纵向业务链路，小步扩展。

## 文档入口

- [AI 协作快速入口](./docs/AI_DEV_GUIDE.md)
- [任务阅读路由](./docs/operations/task-entry-points.md)
- [架构总览](./docs/architecture/overview.md)
- [模块边界](./docs/architecture/module-boundaries.md)
- [公开端口索引](./docs/contracts/module-ports.md)
- [本地开发](./docs/operations/local-dev.md)

## 许可证

当前仓库的自有代码尚未选择开源许可证。公开可见不代表已经授予复制、修改或分发许可；许可证由用户后续决定。

仓库包含 Apache Maven Wrapper 启动脚本，该第三方组件按 Apache License 2.0 分发，范围与通知见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) 和 [LICENSES/Apache-2.0.txt](./LICENSES/Apache-2.0.txt)。该第三方许可不适用于仓库自有代码。
