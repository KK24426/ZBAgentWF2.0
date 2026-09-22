# ADR 0001：采用 Java 21 和 Maven

- 状态：superseded
- 替代决策：[ADR 0004：Java 26](./0004-java-26.md) 替代 Java 21；[ADR 0006](./0006-single-project-spring-mysql.md) 替代四模块和无 Spring 方案。Maven/Wrapper 基线继续有效。
- 日期：2026-08-09
- 决策人：zebiao

> 当前适用范围（2026-09-23）：下文保留历史原文，不作为恢复 Java 21、四模块或无框架方案的依据。当前架构以 ADR 0006 为准。

## 背景

用户希望使用自己熟悉的 Java 重新掌握项目开发过程，并亲自编写框架和公共接口。

## 决策

- 使用 Java 21 作为语言和编译基线；
- 使用 Maven 3.9.16 聚合四个模块；
- 提交 Maven Wrapper only-script；Windows 只需预装 JDK，macOS/Linux 首次下载还需 `unzip` 及 `sha256sum` 或 `shasum`；
- 当前不引入 Spring 或其它应用框架。

## 影响

- 用户可以直接理解和维护核心代码；
- AI 实现被限制在清晰的 Java 模块和接口之后；
- 后续若更换 Java 版本、Maven 或引入框架，需要用户新决策。
