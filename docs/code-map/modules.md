# 包路由
单 Maven 工程，文件名为历史入口。

| 任务 | 位置 | 验证 |
| --- | --- | --- |
| 用户接口、编排 | user | 目标测试、调用方、verify |
| 接口与数据库实现 | agent | 目标测试、调用方、verify；数据库用mysql-it |
| 单次聊天骨架 | user.chat、agent.chat、common.chat、common.exception、static/chat.js | Service/Controller替身、过滤器/日志、真实JAR未接入路径 |
| Bean、工具、共享技术异常 | common | 目标测试、调用方、verify |
| Web启动、首页、配置 | 根包、agent.web及resources | 单元测试、真实Web JAR IT、verify |

开始任务先读根AGENTS.md、就近AGENTS.md和根REQUIREMENTS.md。
