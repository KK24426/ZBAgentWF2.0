# 接口与配置

暂无业务公开接口、DTO或schema。根包启动入口为 com.kk24426.zbagentwf.ZbAgentWfCli。
正式调用为 java -jar target/zbagentwf-cli-<version>.jar <command> [arguments]。
help/--help/version/--version 区分大小写，不接受额外参数；无参数显示帮助。
stdout仅命令结果，stderr包含日志、固定错误提示及脱敏异常链。
退出码0成功、1初始化/执行/日志故障、2用法错误。未知原始输入不进入任何日志。

## 配置入口
| 配置 | 来源与默认 |
| --- | --- |
| Spring属性 | 标准外部配置、环境变量、JVM -D；CLI业务参数不作为配置 |
| mysql开关 | spring.profiles.active=mysql 或 SPRING_PROFILES_ACTIVE=mysql；默认关闭 |
| 数据源 | spring.datasource.url/username/password，支持 SPRING_DATASOURCE_* |
| 日志根目录 | JVM -Dzb.log-dir 优先，其次 ZB_LOG_DIR，最后工作目录logs |
| 日志级别 | logging.level.root=INFO、logging.level.com.kk24426.zbagentwf=DEBUG |
| 文件大小 | JVM -Dzb.log-max-size=20MB，测试可调小；按日及大小滚动 |
| 测试库 | ZB_TEST_DB_URL、ZB_TEST_DB_USERNAME、ZB_TEST_DB_PASSWORD；无生产配置回退 |

mysql模式不自动建库/建表/迁移；无模式时自动数据源配置显式排除。
MyBatis Mapper由Spring注册；配置限制在agent.persistence.mapper，XML在资源mapper目录。
数据源用户名密码不能为空，Hikari上限5、空闲0、取连接超时5秒、驱动读取超时30秒。
日志同进程runId固定；异常类型/堆栈/cause/suppressed保留，消息已知敏感字段脱敏。
文件日志使用UTF-8；控制台日志跟随System.err编码，与固定错误提示保持一致。
MyBatis原生日志关闭，诊断拦截器只记录statement ID、操作、耗时、结果和异常；不输出参数和SQL文本。

包内CliApplication不构成业务接口；common.logging公开类型和MySqlConfiguration属于已批准的技术初始化能力。
后续用户业务接口、DTO、schema、事务变化在本文同步索引，不能由文档先发明契约。
