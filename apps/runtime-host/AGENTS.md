# Runtime Host Module Rules

本模块是未来的进程入口和组合根，当前没有启用任何协议或运行能力。

- 开始任务前读取本文件、`REQUIREMENTS.md`、application/adapters 公开出口及相关契约；必须先理解用户最新修改的 host、协议和配置意图。
- AI 只有在用户明确选择 host 形式、协议和配置契约后才能新增入口代码。
- 本模块可以组装 adapters、application 和 domain，但不得承载领域规则。
- 不得擅自选择 HTTP、stdio、桌面 IPC、Spring Boot 或其它 host 技术。
- 启动进程、读取配置、日志落盘、网络监听和关闭资源都属于显式副作用。
- 协议或配置不足时停止并提交建议，不得自行发明兼容层。
- 适用时保留启动阶段、退出码、超时/取消和脱敏摘要；新增、移动或删除源码时同步 `docs/code-map/files.md`。
- 范围、验收或待确认事项变化时同步 `REQUIREMENTS.md`。
