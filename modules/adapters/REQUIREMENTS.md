# Adapters Requirements

## 当前范围

- 实现 `application` 已批准出站端口的外部适配。
- 当前没有数据库、Agent、HTTP、文件或 Git adapter。

## 验收基线

- adapter 不承载或复制领域决策，SDK 类型和外部数据结构不泄漏到内层。
- 副作用、目标环境、资源释放、超时、取消和错误映射可见并可验证。
- 日志、错误和测试证据不包含敏感信息。

## 待用户确认

- 第一项外部能力、对应 application 端口、技术选型和目标环境。
