# 架构总览

单 Maven 工程，按包组织，不采用 Maven 子模块隔离。
根包为 com.kk24426.zbagentwf，根启动类初始化日志与非 Web Spring 容器。
user 放用户接口/编排，agent 放接口实现和持久化，common 放共享 Bean/工具。
启动 -> 独立日志 -> Spring扫描/注入 -> CLI执行 -> 关闭容器/连接池 -> 结束日志 -> 退出。

Spring Boot 4.1.1；MyBatis Starter 4.1.0；Java26；Maven Wrapper3.9.16。
未激活 mysql 时没有 DataSource；激活后验证配置和连接，失败退出1。
目前仅框架能力，无生产业务类型、业务表或业务命令。
旧项目不自动搬运；第一条业务链路仍由用户定义。
决策依据见 [ADR0006](../decisions/0006-single-project-spring-mysql.md)。
