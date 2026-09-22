# ADR 0006：单工程、Spring Boot、日志与 MySQL 基础

- 状态：accepted
- 日期：2026-09-22
- 决策人：zebiao
- 替代：0001/0004中的多模块、无框架部分；0005中的纯Java初始化和Shade分发部分。

用户明确“模块”是包，实验项目无需多Maven模块；选择user/agent/common、Spring Boot和注解注入。
common由双方补充，但业务接口、核心规则、schema和事务边界继续由用户把控。
使用Boot4.1.1、MyBatis Starter4.1.0，Connector/J由Boot管理。
单JAR通过Boot repackage分发，根main负责初始化、执行和关闭。
默认无数据库，mysql profile才建立Hikari/MyBatis/事务支持；先框架后业务表。
SQL和Mapper实现交给Agent，用户指定业务输入输出和表结构。
日志项目DEBUG/框架INFO，stderr诊断、stdout结果，runId独立UTF8文件，20MB/日滚动，不自动删除。
完整异常链需要与脱敏同时保留；不打印参数/SQL参数/结果集。
MySQL验证用本机专用库，未有环境时如实标记待验收，不安装Docker或探测真实库。
保留双阶段独立review、用户修改checkpoint和已评审后自动commit/push。
不增加Web服务、迁移工具、代码生成器、静态Spring容器或生产演示表。

代价：包界限依靠协作规则，Boot有初始化成本，日志需要用户清理，MySQL实际验收依赖专用环境。
