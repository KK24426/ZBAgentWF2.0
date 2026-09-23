# 架构总览

单 Maven 工程，按包组织，不采用 Maven 子模块隔离。
根包为 com.kk24426.zbagentwf，根启动类初始化日志与 Spring Web 容器。
user 放用户接口/编排，agent 放接口实现和持久化，common 放共享 Bean/工具及跨包使用的技术异常。
启动 -> 独立日志 -> Spring扫描/注入 -> 内嵌Tomcat持续处理HTTP -> 停止信号 -> 优雅关闭容器/连接池和日志。

Spring Boot 4.1.1；MyBatis Starter 4.1.0；Java26；Maven Wrapper3.9.16。
未激活 mysql 时没有 DataSource；激活后验证配置和连接，失败退出1。
当前首页包含单次聊天入口：ChatController -> ChatService -> AgentChat -> agent.chat.AgentChatImpl。正式实现明确尚未接入；成功路径由测试替身验证，无业务表或真实Agent执行。
WebRequestFilter限制固定资源和/api/chat POST、记录安全请求摘要；后续路由仍须按批准契约同步调整。
远程测试服务与专用MySQL库部署在同一已批准服务器，Web只绑定回环地址，通过SSH隧道访问。
旧项目不自动搬运；后续真实Agent调用、会话、数据存储仍由用户定义。
决策依据见 [ADR0006](../decisions/0006-single-project-spring-mysql.md) 和 [ADR0007](../decisions/0007-web-test-service.md)。
