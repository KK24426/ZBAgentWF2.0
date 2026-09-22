# 架构总览

单 Maven 工程，按包组织，不采用 Maven 子模块隔离。
根包为 com.kk24426.zbagentwf，根启动类初始化日志与 Spring Web 容器。
user 放用户接口/编排，agent 放接口实现和持久化，common 放共享 Bean/工具。
启动 -> 独立日志 -> Spring扫描/注入 -> 内嵌Tomcat持续处理HTTP -> 停止信号 -> 优雅关闭容器/连接池和日志。

Spring Boot 4.1.1；MyBatis Starter 4.1.0；Java26；Maven Wrapper3.9.16。
未激活 mysql 时没有 DataSource；激活后验证配置和连接，失败退出1。
目前仅框架与静态首页，无生产业务类型、业务表或业务API。
WebRequestFilter限制固定资源和方法、记录安全请求摘要；新增业务路由时须按批准契约同步调整。
远程测试服务与专用MySQL库部署在同一已批准服务器，Web只绑定回环地址，通过SSH隧道访问。
旧项目不自动搬运；第一条业务链路仍由用户定义。
决策依据见 [ADR0006](../decisions/0006-single-project-spring-mysql.md) 和 [ADR0007](../decisions/0007-web-test-service.md)。
