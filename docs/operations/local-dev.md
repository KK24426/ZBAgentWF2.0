<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-09
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：说明 Java 21 Maven 工程的本地开发和验证命令。
 -->

# Local Development

## 环境

- JDK 21；
- Windows 使用 `mvnw.cmd`；
- macOS/Linux 使用 `./mvnw`，首次下载还需要 `unzip`，以及 `sha256sum` 或 `shasum`。

Maven Wrapper 固定 Maven 3.9.16。首次执行会从 Maven Central 下载 Maven，下载内容进入用户 Maven 缓存，不进入仓库。

当前只在 Windows 上完成了 Wrapper 构建验证；macOS/Linux 的命令和前置条件已记录，但尚未完成对应环境的首次下载 smoke。

## 命令

```powershell
.\mvnw.cmd verify
```

指定模块及其依赖：

```powershell
.\mvnw.cmd -pl modules/application -am test
```

当前没有业务测试；`verify` 主要确认 Java/Maven 版本、POM 聚合、模块依赖和源码编译。新增功能后必须补充对应测试。

## 环境边界

当前没有数据库、Agent provider、远程服务或部署环境。任何 migration、写库、远程调用和真实 Agent 执行前，必须先由用户确认目标与配置来源。
