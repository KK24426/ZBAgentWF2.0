# 接口与配置

暂无带业务方法的公开接口或持久化 schema；用户已提供空父接口及 Agent 数据骨架，见[用户骨架契约](./user-ports.md)。根包启动入口为 com.kk24426.zbagentwf.ZbAgentWfApplication。
正式调用为 java -jar target/zbagentwf-web-<version>.jar，无命令参数，持续运行。
stderr包含日志、固定错误提示及脱敏异常链，stdout无业务输出。
启动失败退出1、传入任意命令参数退出2且不回显；正常停止释放资源，不立即关闭刚启动的容器。

## HTTP入口

仅GET/HEAD访问 /、/index.html、/app.css、/favicon.svg；其它方法405，未知路径404。
错误体为固定文字，HEAD无响应体；每次请求返回服务端生成的X-Request-ID。
页面包含CSP、nosniff、no-referrer保护，资源不依赖外部站点，禁用缓存。
请求日志只含白名单方法、固定类别、状态和耗时；不记录原始URL/查询/正文。
Tomcat协议组件的解析前诊断采用固定摘要，异常类型/堆栈/cause/suppressed保留、原始消息隐藏；解析前拒绝的请求没有应用requestId。
运行期检测到日志故障后，后续请求503；恢复服务需解决日志故障并重启。
当前无登录系统或业务API，默认回环监听；公网开放和业务路由另行批准。

## 配置入口
| 配置 | 来源与默认 |
| --- | --- |
| Spring属性 | 标准外部配置、环境变量、JVM -D；不接受命令参数配置 |
| Web监听 | server.address=127.0.0.1、server.port=8080；环境变量SERVER_ADDRESS、SERVER_PORT |
| 关闭窗口 | server.shutdown=graceful、spring.lifecycle.timeout-per-shutdown-phase=20s |
| mysql开关 | spring.profiles.active=mysql 或 SPRING_PROFILES_ACTIVE=mysql；默认关闭 |
| 数据源 | spring.datasource.url/username/password，支持 SPRING_DATASOURCE_* |
| 日志根目录 | JVM -Dzb.log-dir 优先，其次 ZB_LOG_DIR，最后工作目录logs |
| 日志级别 | logging.level.root=INFO、logging.level.com.kk24426.zbagentwf=DEBUG |
| 文件大小 | JVM -Dzb.log-max-size=20MB，测试可调小；按日及大小滚动 |
| 测试库 | ZB_TEST_DB_URL、ZB_TEST_DB_USERNAME、ZB_TEST_DB_PASSWORD；无生产配置回退 |

mysql模式不自动建库/建表/迁移；无模式时自动数据源配置显式排除。
MyBatis Mapper 仅在 mysql profile 下由 Spring 扫描注册；接口位于 com.kk24426.zbagentwf.agent.persistence.mapper 包及其子包且必须标注 @Mapper，XML 位于资源 mapper 目录。对应写法见 [Mapper 示例](../../src/main/resources/mapper/README.md)。
数据源用户名密码不能为空，Hikari上限5、空闲0、取连接超时5秒、驱动读取超时30秒。
日志同进程runId固定；异常类型/堆栈/cause/suppressed保留，消息已知敏感字段脱敏。
文件日志使用UTF-8；控制台日志跟随System.err编码，与固定错误提示保持一致。
MyBatis原生日志关闭，诊断拦截器只记录statement ID、操作、耗时、结果和异常；不输出参数和SQL文本。

ZbAgentWfApplication、WebRequestFilter、common.logging公开类型和MySqlConfiguration属于已批准的技术基础能力，不构成用户业务接口。
后续用户业务接口、DTO、schema、事务变化在本文同步索引，不能由文档先发明契约。
