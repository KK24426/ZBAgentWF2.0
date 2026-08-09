# Domain Module Rules

本模块由用户主导，承载领域对象、值对象、业务规则、领域错误和状态机。

- 开始任务前读取本文件、`REQUIREMENTS.md`、真实 Java 源码及相关契约；用户修改的接口和规则是 AI 实现的当前输入。
- AI 默认只能读取本模块。
- 未经用户明确要求，不得新增或修改这里的 Java 类型、公共方法、状态转换或业务不变量。
- 本模块不得依赖 `application`、`adapters`、`runtime-host`、数据库、网络、文件系统或 Agent SDK。
- 用户授权修改时，先复述规则、输入输出和失败语义，再实施并补充纯单元测试。
- 发现领域定义不足时停止并提出方案，不得在 adapter 中复制或绕过领域规则。
- 新增、移动或删除源码时同步 `docs/code-map/files.md`；范围、验收或待确认事项变化时同步 `REQUIREMENTS.md`。
