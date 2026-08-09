<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：提供任务到 Java 模块的首层路由。
 -->

# Module Routing

| 任务类型 | 首选模块 | 开始前要求 | 默认验证 |
| --- | --- | --- | --- |
| 领域对象、规则、状态机 | `domain` | 用户明确编写或授权；读取就近 `REQUIREMENTS.md` | 纯单元测试 -> 直接调用者 -> `mvnw.cmd verify` |
| 用例接口、出站端口、事务意图 | `application` | 用户明确编写或授权；读取就近 `REQUIREMENTS.md` | 用例测试 -> 直接调用者 -> `mvnw.cmd verify` |
| 数据库、Agent、文件、网络、Git 接入 | `adapters` | 对应端口已批准；读取就近 `REQUIREMENTS.md` | adapter 测试 -> runtime-host -> `mvnw.cmd verify` |
| 进程入口、配置和对象组装 | `runtime-host` | host 与协议已批准；读取就近 `REQUIREMENTS.md` | 启动/关闭测试 -> `mvnw.cmd verify` |

无法定位时先询问用户，不新增“通用模块”接住不清楚的职责。
