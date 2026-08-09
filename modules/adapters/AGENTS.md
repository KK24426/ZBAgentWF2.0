# Adapters Module Rules

本模块是 AI 默认实现区域，用于实现 application 已定义的出站端口。

- 开始任务前读取本文件、`REQUIREMENTS.md`、application 真实公开出口及相关契约；必须先理解用户最新修改的端口和错误语义。
- 当前没有启用数据库、Agent provider、HTTP、文件或 Git adapter；不得把候选技术写成已实现事实。
- 新增 adapter 前必须确认对应 application 端口已由用户定义或批准。
- adapter 只负责协议翻译、数据映射、资源管理和错误保真，不承载领域决策。
- 不得反向修改 domain/application 公共接口来迁就具体 SDK。
- 外部进程使用结构化参数；日志和错误必须脱敏；副作用和目标环境必须显式。
- 适用时保留错误来源、模块、阶段、业务 ID、退出码、超时/取消和脱敏摘要，不吞掉外部异常。
- 新增、移动或删除源码时同步 `docs/code-map/files.md`；范围、验收或待确认事项变化时同步 `REQUIREMENTS.md`。
