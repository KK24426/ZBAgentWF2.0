# 包边界

本文件沿用原文件名供历史链接定位；现在不再使用 Maven 子模块。

| 位置 | 职责 |
| --- | --- |
| 根包 | main、Web启动初始化、生命周期 |
| user | 用户编写的接口、编排、部分实现；已授权的user.chat Controller/Service及AgentChat调用契约 |
| agent | Agent实现，含agent.chat占位实现、Web请求保护与日志、persistence配置、Mapper与SQL诊断 |
| common | 共享Bean、DTO、工具、日志辅助，以及common.exception中的Agent不可用异常 |

本轮在既有职责内补充：根包 ProjectConfiguration 负责项目根目录初始化；common.project.bean 包含项目/需求/Task/状态及只读配置，common.agent.bean 包含模型信息与执行结果；user.project.userif、user.agent.userif 声明项目与执行能力。真实项目操作、CLI 和调度未实现。

这些是协作边界，不强制字节码依赖隔离。通过用户接口注入实现。
Agent 可补充技术Mapper和任务所需工具；业务接口、schema、核心规则和事务边界必须由用户定义或批准。
不预建通用CRUD基类、业务表或额外架构层。权限唯一权威为根AGENTS.md。
