# 包边界

本文件沿用原文件名供历史链接定位；现在不再使用 Maven 子模块。

| 位置 | 职责 |
| --- | --- |
| 根包 | main、Web启动初始化、生命周期 |
| user | 用户编写的接口、编排、部分实现；含聊天和项目 Service 及调用契约 |
| agent | Agent实现，含聊天和项目 Controller、agent.chat占位实现、Web请求保护与日志、persistence配置、Mapper与SQL诊断 |
| common | 共享Bean、DTO、工具、日志辅助，以及common.exception中的Agent不可用异常 |

根包 ProjectConfiguration 负责项目根目录初始化；common.project.model 包含项目/需求/Task/状态及角色，common.project.config 保存只读项目配置，common.project.dto 保存 HTTP 输入与响应快照，common.agent.model 包含模型信息与结果；user.project.api、user.agent.api、user.chat.api 声明用户契约。agent.codex 实现本机 Codex 进程、异步执行及只读规划，agent.project 实现项目目录、需求追加和串行 Task 执行；agent.registry 实现用户新增 AgentExecutorFactory、AgentRegistry 和规划适配，agent.runtime 保存执行器公共实现并管理共享额度及关闭，根包 AgentConfiguration 负责装配。用户批准 Project 通过 user.agent.api.AgentExecutor 绑定三个角色；common 项目实体对该用户抽象的运行时引用是本次明确授权的依赖，不推广为其它包依赖许可。

这些是协作边界，不强制字节码依赖隔离。通过用户接口注入实现。
2026-09-27批准本地化：根包MsgConfiguration装配；common.msg提供消息读取、校验和纯文本查询；agent.web承担请求语言与首页适配；agent.chat.controller仅注入共用MsgCatalog查询固定提示，不依赖agent.web具体类型。无业务接口/DTO调整。
Agent 可补充技术Mapper和任务所需工具；业务接口、schema、核心规则和事务边界必须由用户定义或批准。
不预建通用CRUD基类、业务表或额外架构层。权限唯一权威为根AGENTS.md。

2026-09-27批准项目Controller/Service编排、common.memory通用对象存储及ProjectHttp快照、agent.prompt规则加载和agent.registry角色绑定。随后按用户要求，将聊天和项目Controller分别归入agent.chat.controller、agent.project.controller，继续调用user中的Service；common保留共享数据和工具。ProjectDomain继续由用户定义，没有引入DAO/schema。
