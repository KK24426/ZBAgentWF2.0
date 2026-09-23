<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：索引 ZBAgentWF2.0 的长期架构决策。
 -->

# Architecture Decision Records

- [0001：采用 Java 21 和 Maven（Java 版本及架构分别由 0004、0006 替代；Maven/Wrapper 基线保留）](./0001-java-maven.md)
- [0002：用户拥有架构和公共接口决策权](./0002-human-owned-architecture.md)
- [0003：按纵向业务链路小步开发](./0003-vertical-slice-development.md)
- [0004：升级到 Java 26（版本基线有效，无框架部分已被 0006 替代）](./0004-java-26.md)
- [0005：CLI 单 JAR 入口（CLI由0007替代，旧模块、初始化及Shade由0006替代）](./0005-cli-jar-distribution.md)

- [0006：单工程、Spring Boot、日志与 MySQL 基础](./0006-single-project-spring-mysql.md)
- [0007：Web服务与远程专用测试环境](./0007-web-test-service.md)
- [0008：单次聊天功能切片与测试替身边界](./0008-chat-skeleton.md)

阅读历史 ADR 时先看顶部的当前适用范围；已被替代的正文只保留决策背景，不用于恢复旧架构。当前按包协作，不因历史“模块”措辞新建 Maven 子模块；用户权限仍以根 `AGENTS.md` 为准。

新的长期技术选择使用 [TEMPLATE.md](./TEMPLATE.md) 记录。候选想法不写成 accepted；先标记 proposed 并由用户决定。
