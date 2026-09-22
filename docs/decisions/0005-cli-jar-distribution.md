# ADR 0005：以纯 Java CLI 单 JAR 作为首个运行入口

- 状态：accepted
- 日期：2026-08-19
- 决策人：zebiao

## 背景

项目需要先形成可运行、可验证且便于脚本调用的最小交付物，同时继续由用户掌握业务接口和业务链路。当前没有真实业务命令，也没有引入应用框架的必要。

## 决策

- 使用 `apps/runtime-host` 作为 CLI 进程入口和未来组合根，不新增 Maven 模块；
- 使用纯 Java 实现命令解析，当前只提供帮助和版本命令，不引入 Spring、Picocli 或 Agent SDK；
- 使用 Maven Shade 在 `package` 阶段生成并替换主产物，正式分发物为 `target/zbagentwf-cli-${project.version}.jar`；
- CLI 正常输出写 stdout，错误写 stderr，退出码 `0`、`1`、`2` 分别表示成功、功能执行失败、命令或参数错误；
- runtime-host 定位为 CLI-only 分发，不承诺作为 Maven 类库使用；Maven 坐标和模块依赖方向保持不变。

## 影响

- 项目可以通过 `java -jar` 直接调用和执行真实进程级 smoke；
- 后续业务命令必须调用用户定义的 application 接口，不得在 CLI 层承载业务规则；
- `original-zbagentwf-cli-*.jar` 只是 Shade 构建中间物，不属于正式分发；
- 新增命令、参数、配置或其它协议时仍需用户批准公共边界。

## 备选方案

- 新增独立 CLI Maven 模块：当前规模不足，且会改变用户掌握的模块结构，未采用。
- 引入 Picocli 或 Spring Boot：内建命令过少，额外框架暂时没有收益，未采用。
- 分发普通 thin JAR：会增加调用方组装 classpath 的负担，不符合单 JAR 调用目标，未采用。

> 2026-09-22：多模块、无 Spring 和 Shade 分发相关内容已被 [ADR 0006](./0006-single-project-spring-mysql.md) 替代；本文件保留历史。Java 26 与 Maven Wrapper 3.9.16 基线继续有效。
