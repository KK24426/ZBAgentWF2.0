# 单次聊天契约

用户批准：2026-09-24，在 user 下建立功能包/controller/service，由 agent 包实现调用；首版为骨架及测试替身验证，并提供简单网页。无真实模型调用、数据库或会话历史。

## Java 出口

| 类型 | 职责 |
| --- | --- |
| user.chat.controller.ChatController | HTTP请求/响应及固定错误映射 |
| user.chat.service.ChatService extends UserService | 校验本次请求并通过构造注入的AgentChat委派 |
| user.chat.service.AgentChat extends UserImpl | String reply(String message)，接收原始有效文本并返回回复 |
| user.chat.service.AgentUnavailableException | Agent尚未接入或不可用，映射503 |
| agent.chat.AgentChatImpl extends AgentBase | 正式占位实现，仅明确不可用，不产生模拟成功 |
| common.chat.ChatRequest / ChatResponse | 不可变请求message与响应reply载体，不注册为Spring组件 |

UserService、UserImpl、AgentBase、AgentBean 原有定义不变。接口实现可以在测试中注入替换；测试成功不代表真实 Agent 已接入。技术性类不代表授权扩展业务字段或模型调用协议。

## HTTP

`POST /api/chat`，Content-Type 为 `application/json`；一次请求、一次结果。

```json
{"message":"请帮我整理工作计划。"}
```

message必须为非空白字符串，不超过4000个Java UTF-16代码单元（与浏览器maxlength计数一致）；不自动trim或规范化有效输入。仅处理message，不引入其它字段的业务含义。

成功响应为200及 `application/json`：

```json
{"reply":"测试中由替身提供的回复"}
```

该示例仅解释字段，不是正式服务的固定回复。生产占位会返回503。

| 情况 | HTTP状态 |
| --- | --- |
| 成功（本轮仅通过测试替身验证） | 200 |
| 请求缺失、JSON格式/字段类型错误、message空白或过长 | 400 |
| /api/chat使用非POST方法 | 405，Allow: POST |
| 非JSON媒体类型 | 415 |
| Agent未接入或服务暂不可用 | 503 |
| 内部错误 | 500 |

错误使用UTF-8固定文字，不回显请求、回复或异常；HEAD错误无正文。每个进入过滤器的请求带服务端X-Request-ID；未知路径和原静态资源的拒绝行为保持。

## 页面与诊断

首页中的输入、发送按钮和结果区调用同源接口。请求期间阻止重复发送，结果通过textContent显示，成功/失败均恢复操作；结果可带请求编号。浏览器等待30秒超时后显示固定提示，不自动重试、不持久化输入或结果，也不代表服务端取消协议。

仅开放同源chat.js脚本和同源连接；其余CSP保护维持原有边界。日志只记白名单方法、固定CHAT类别、状态及耗时，不记录正文/回复；聊天异常和该请求中的Spring诊断隐藏消息原文，保留异常结构和完整调用链。

真实Agent接入、模型选择、鉴权、执行超时/取消、自动重试及会话历史仍待定义。
