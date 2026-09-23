# ZBAgentWF2.0

这是一个实验性的 Java Web 项目：你定义接口与业务，Agent 实现具体功能。
一个 Maven 工程，使用包区分协作职责：

- `user`：你的接口、编排与手写业务代码。
- `agent`：Agent 实现、MyBatis Mapper 和 SQL。
- `common`：双方维护的 Bean、DTO 和工具。

基线：Java 26、Maven Wrapper 3.9.16、Spring Boot 4.1.1、MyBatis Starter 4.1.0、MySQL Connector/J。
默认无数据库，远程测试环境启用专用 MySQL；没有生产业务表或业务保存功能。
首页提供单次请求输入与结果区；后端为 Controller → Service → Agent 接口骨架。真实 Agent 尚未接入，正式运行返回明确不可用提示；成功路径仅用测试替身验证，见[聊天契约](./docs/contracts/chat.md)。

## 构建与运行

```powershell
.\mvnw.cmd clean verify
java -jar .\target\zbagentwf-web-0.1.0-SNAPSHOT.jar
```

浏览器打开 http://127.0.0.1:8080/。Eclipse 可直接运行根包的 `ZbAgentWfApplication.main`。
进程持续运行，正常停止使用 Ctrl+C；CLI 及 help/version 已移除，不接受命令参数。
默认仅监听本机，远程测试服务通过 SSH 隧道访问，见[远程测试环境](./docs/operations/remote-test.md)。

默认不连接数据库；stderr 输出日志，不向 stdout 输出业务结果。启动失败退出1、传入命令参数退出2。
日志默认位于运行工作目录的 `logs/<runId>/application.log`；每次运行独立、文件UTF-8、20MB/日滚动，不自动清理。
默认项目DEBUG、框架INFO；请求日志包含 requestId、固定路由类别、状态和耗时，不打印原始路径、查询或 SQL 参数。
启动日志故障时失败，运行期检测到日志故障后后续请求返回503；正常关闭时刷新。

## Spring 注入
在 user 中定义接口，agent 中用 `@Service` 实现；你自己的调用类也标注 `@Component`，
然后通过构造器注入接口。两个实现通过 `@Qualifier` 或 `@Primary` 选择。
common 中 Bean 不必全部注册成 Spring Bean，普通数据对象可以直接创建。

## 开发入口

- [协作规则](./AGENTS.md)、[当前需求](./REQUIREMENTS.md)
- [本地开发、Eclipse、日志和 MySQL 配置](./docs/operations/local-dev.md)
- [最小注入示例](./docs/operations/spring-example.md)
- [Mapper 注解与 XML 对应示例](./src/main/resources/mapper/README.md)
- [包边界](./docs/architecture/module-boundaries.md)、[文件索引](./docs/code-map/files.md)
- [契约与配置](./docs/contracts/module-ports.md)

## 许可证
自有代码尚未选择开源许可证，公开可见不等于授予使用许可。
Maven Wrapper 第三方通知见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) 和 [Apache-2.0](./LICENSES/Apache-2.0.txt)。
