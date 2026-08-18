<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-19
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：维护用户批准后的跨模块公开端口与进程入口索引。
 -->

# Module Ports

## 当前状态

当前没有业务公开端口、DTO、事件或状态枚举。`runtime-host` 已有用户批准的 CLI 进程入口；四个 `package-info.java` 只声明包边界，不是公开契约。

## CLI 进程入口

- 入口类：`com.kk24426.zbagentwf.runtime.ZbAgentWfCli`；
- 调用方式：`java -jar zbagentwf-cli-<version>.jar <command> [arguments]`；
- 内建命令：`help`、`--help`、`version`、`--version`，区分大小写且不接受额外参数；
- 输出约定：正常结果写 stdout，错误写 stderr；
- 退出码：`0` 成功、`1` 功能执行失败、`2` 命令或参数错误；
- 安全边界：未知命令不回显原始输入，内部异常不输出原始消息或堆栈。

完整精确输出和分发边界以 `apps/runtime-host/REQUIREMENTS.md`、入口测试及 [ADR 0005](../decisions/0005-cli-jar-distribution.md) 为准。

## 记录规则

用户创建或批准第一个公共接口后，在这里记录：

- Java 完整限定名；
- 接口所有者模块；
- 调用方向；
- 输入、输出和错误语义；
- 是否涉及状态、事务或副作用；
- 兼容性承诺和验证入口。

字段级事实以用户编写的 Java 接口及其测试为准。AI 不得先在本文发明契约，再据此生成接口。

## 当前依赖索引

| 调用模块 | 可依赖模块 | 当前公开端口 |
| --- | --- | --- |
| `application` | `domain` | 无，待用户定义 |
| `adapters` | `application`、`domain` | 无，待用户定义 |
| `runtime-host` | `adapters`、`application`、`domain` | 尚无业务端口；已有 CLI 进程入口 |
