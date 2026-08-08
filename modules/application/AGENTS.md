# Application Module Rules

本模块由用户主导，承载用例接口、用例编排和内层需要的出站端口。

- AI 默认只能读取公共接口和端口定义。
- 未经用户明确要求，不得创建或修改公共接口、`public` DTO、事务语义或出站端口。
- 本模块只允许依赖 `domain`，不得依赖 adapter、具体数据库、HTTP client、Agent SDK 或 runtime-host。
- AI 可以在用户批准的接口后实现包内用例类，但接口不足时必须先停下提议。
- 用例实现需要覆盖主路径、失败路径和用户给出的验收示例。
