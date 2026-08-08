# Runtime Host Module Rules

本模块是未来的进程入口和组合根，当前没有启用任何协议或运行能力。

- AI 只有在用户明确选择 host 形式、协议和配置契约后才能新增入口代码。
- 本模块可以组装 adapters、application 和 domain，但不得承载领域规则。
- 不得擅自选择 HTTP、stdio、桌面 IPC、Spring Boot 或其它 host 技术。
- 启动进程、读取配置、日志落盘、网络监听和关闭资源都属于显式副作用。
- 协议或配置不足时停止并提交建议，不得自行发明兼容层。
