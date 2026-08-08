# Adapters Module Rules

本模块是 AI 默认实现区域，用于实现 application 已定义的出站端口。

- 当前没有启用数据库、Agent provider、HTTP、文件或 Git adapter；不得把候选技术写成已实现事实。
- 新增 adapter 前必须确认对应 application 端口已由用户定义或批准。
- adapter 只负责协议翻译、数据映射、资源管理和错误保真，不承载领域决策。
- 不得反向修改 domain/application 公共接口来迁就具体 SDK。
- 外部进程使用结构化参数；日志和错误必须脱敏；副作用和目标环境必须显式。
