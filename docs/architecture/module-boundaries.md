<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-19
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：定义四个 Java 模块的职责、单向依赖和 CLI 边界。
 -->

# Module Boundaries

## 依赖方向

```text
runtime-host
  -> adapters
      -> application
          -> domain
```

允许的直接依赖：

- `domain`：不依赖其它项目模块；
- `application`：只依赖 `domain`；
- `adapters`：依赖 `application` 和 `domain`；
- `runtime-host`：依赖 `adapters`、`application` 和 `domain`。

## Domain

只表达纯业务语义、规则、不变量和状态转换。不访问数据库、网络、文件、进程、时钟实现、Agent SDK 或 UI。

## Application

表达业务用例、输入输出边界、事务意图和内层需要的外部能力端口。端口由需要能力的内层定义，由 adapter 实现。

## Adapters

把数据库、文件、网络、Agent、Git 或其它外部系统翻译为 application 端口。不得让 SDK 类型、数据库 row 或进程 handle 泄漏到内层。

## Runtime Host

只负责 CLI 命令解析、配置读取、对象组装、生命周期和进程协议接入。当前已批准的进程入口仅为 CLI，内建命令只有帮助和版本；新增业务命令或其它协议仍须用户批准。

## 修改边界

模块、公共接口、DTO、状态机、协议和依赖方向属于用户决策。AI 可以提出带影响分析的修改建议，但必须等待明确批准。
