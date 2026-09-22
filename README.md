# ZBAgentWF2.0

这是一个实验性的 Java CLI 项目：你定义接口与业务，Agent 实现具体功能。
一个 Maven 工程，使用包区分协作职责：

- `user`：你的接口、编排与手写业务代码。
- `agent`：Agent 实现、MyBatis Mapper 和 SQL。
- `common`：双方维护的 Bean、DTO 和工具。

基线：Java 26、Maven Wrapper 3.9.16、Spring Boot 4.1.1、MyBatis Starter 4.1.0、MySQL Connector/J。
运行 MySQL 尚需配置；没有生产业务表或业务保存功能。

## 构建与运行
```powershell
.\mvnw.cmd clean verify
java -jar .\target\zbagentwf-cli-0.1.0-SNAPSHOT.jar help
java -jar .\target\zbagentwf-cli-0.1.0-SNAPSHOT.jar version
```

Eclipse 可直接运行根包的 `ZbAgentWfCli.main`，默认显示帮助、初始化 Spring 并记录日志。
版本命令依赖打包 Manifest，IDE 直接运行 version 缺少 Manifest 时返回1，不伪造版本。

默认不连接数据库。stdout 输出结果，stderr 输出日志；退出码0成功、1失败、2参数错误。
日志默认位于运行工作目录的 `logs/<runId>/application.log`；每次运行独立、文件UTF-8、20MB/日滚动，不自动清理。
默认项目DEBUG、框架INFO；SQL参数不打印。每次日志目录故障可见，关闭前刷新。

## Spring 注入
在 user 中定义接口，agent 中用 `@Service` 实现；你自己的调用类也标注 `@Component`，
然后通过构造器注入接口。两个实现通过 `@Qualifier` 或 `@Primary` 选择。
common 中 Bean 不必全部注册成 Spring Bean，普通数据对象可以直接创建。

## 开发入口
- [协作规则](./AGENTS.md)、[当前需求](./REQUIREMENTS.md)
- [本地开发、Eclipse、日志和 MySQL 配置](./docs/operations/local-dev.md)
- [最小注入示例](./docs/operations/spring-example.md)
- [包边界](./docs/architecture/module-boundaries.md)、[文件索引](./docs/code-map/files.md)
- [契约与配置](./docs/contracts/module-ports.md)

## 许可证
自有代码尚未选择开源许可证，公开可见不等于授予使用许可。
Maven Wrapper 第三方通知见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) 和 [Apache-2.0](./LICENSES/Apache-2.0.txt)。
