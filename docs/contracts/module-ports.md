# 接口与配置

已提供[单次聊天骨架](./chat.md)，该聊天入口的真实 Agent 与持久化 schema 尚未接入；原空父接口及数据骨架见[用户骨架契约](./user-ports.md)。根包启动入口为 com.kk24426.zbagentwf.ZbAgentWfApplication。
项目包含需求、需求包含 Task 的实体及执行行为见[项目与执行契约](./project-agent.md)。已有配置驱动的模型注册、执行器工厂与项目三角色装配；已完成当前本机Codex与gpt-6-astra的有限项目功能验收，见[验收记录](../operations/codex-live-validation.md)。
聊天调用的共享技术异常为 common.exception.AgentUnavailableException，供实现层与调用层共同引用。
正式调用为 java -jar target/zbagentwf-web-<version>.jar，无命令参数，持续运行。
stderr包含日志、固定错误提示及脱敏异常链，stdout无业务输出。
启动失败退出1、传入任意命令参数退出2且不回显；正常停止释放资源，不立即关闭刚启动的容器。

## HTTP入口

聊天和项目Controller分别位于agent.chat.controller、agent.project.controller，调用user.chat.service、user.project.service；首页和项目页面Controller位于agent.web。此归包不改变HTTP路径、消息、状态码或DTO字段。

GET/HEAD访问 /、/index.html、/projects、/projects.html、/app.css、/favicon.svg、/chat.js、/messages.js、/projects.js、/projects.css；POST /api/chat 见聊天契约。项目五个精确入口见[项目HTTP契约](./project-agent.md#项目-http-入口)；其余路径/方法仍拒绝。
错误体为按请求语言查询的固定文字，HEAD无响应体；每次请求返回服务端生成的X-Request-ID。页面与接口的消息文件、配置和语言选择见[本地化契约](./msg.md)。
页面包含CSP、nosniff、no-referrer保护，资源不依赖外部站点，禁用缓存。
请求日志只含白名单方法、固定类别、状态和耗时；不记录原始URL/查询/正文。
Tomcat协议组件的解析前诊断采用固定摘要，异常类型/堆栈/cause/suppressed保留、原始消息隐藏；解析前拒绝的请求没有应用requestId。
运行期检测到日志故障后，后续请求503；恢复服务需解决日志故障并重启。
当前无登录系统，默认回环监听；已批准单次聊天及项目创建/查询/提示词/需求/执行入口，公网开放和其它业务路由仍需另行批准。

聊天和项目请求内Spring诊断及相关异常隐藏消息原文并保留完整调用链；无请求上下文的聊天组件异常也适用，普通启动/数据库日志不受影响。

## 配置入口
| 配置 | 来源与默认 |
| --- | --- |
| Spring属性 | 标准外部配置、环境变量、JVM -D；不接受命令参数配置 |
| Agent注册 | config/agents.properties；zb.agents[条目键] 的 brand/name/ver/type/executable/model/timeout/enabled；支持标准字段覆盖，无默认模型，配置修改后重启 |
| 项目根目录 | zb.project.root，启动工作目录下 config/project.properties；支持 ZB_PROJECT_ROOT 和 JVM -Dzb.project.root 覆盖，无默认值且必须显式配置 |
| 默认显示语言 | config/msg.properties 中 zb.msg.locale=auto/zh-CN/en/ja，默认 auto；JVM > ZB_MSG_LOCALE > 文件；手动 Cookie 优先 |
| 消息覆盖 | config/msg/ 中同名 UTF-8 properties 按 key 覆盖内置同语言，修改后重启；真实文件忽略，仅示例入仓 |
| Web监听 | server.address=127.0.0.1、server.port=8080；环境变量SERVER_ADDRESS、SERVER_PORT |
| 关闭窗口 | server.shutdown=graceful、spring.lifecycle.timeout-per-shutdown-phase=20s |
| mysql开关 | spring.profiles.active=mysql 或 SPRING_PROFILES_ACTIVE=mysql；默认关闭 |
| 数据源 | spring.datasource.url/username/password，支持 SPRING_DATASOURCE_* |
| 日志根目录 | JVM -Dzb.log-dir 优先，其次 ZB_LOG_DIR，最后工作目录logs |
| 日志级别 | logging.level.root=INFO、logging.level.com.kk24426.zbagentwf=DEBUG |
| 文件大小 | JVM -Dzb.log-max-size=20MB，测试可调小；按日及大小滚动 |
| 测试库 | ZB_TEST_DB_URL、ZB_TEST_DB_USERNAME、ZB_TEST_DB_PASSWORD；无生产配置回退 |

mysql模式不自动建库/建表/迁移；无模式时自动数据源配置显式排除。
项目根目录缺失、空白、格式非法或已存在非目录时启动失败；相对路径按启动工作目录规范化为绝对路径，允许不存在且初始化不创建目录。优先级 JVM > 环境变量 > 文件，空覆盖值也会失败。
MyBatis Mapper 仅在 mysql profile 下由 Spring 扫描注册；接口位于 com.kk24426.zbagentwf.agent.persistence.mapper 包及其子包且必须标注 @Mapper，XML 位于资源 mapper 目录。对应写法见 [Mapper 示例](../../src/main/resources/mapper/README.md)。
数据源用户名密码不能为空，Hikari上限5、空闲0、取连接超时5秒、驱动读取超时30秒。
日志同进程runId固定；异常类型/堆栈/cause/suppressed保留，消息已知敏感字段脱敏。
文件日志使用UTF-8；控制台日志跟随System.err编码，与固定错误提示保持一致。
MyBatis原生日志关闭，诊断拦截器只记录statement ID、操作、耗时、结果和异常；不输出参数和SQL文本。

ZbAgentWfApplication、ProjectConfiguration、AgentConfiguration、ProjectSettings、WebRequestFilter、common.logging公开类型和MySqlConfiguration属于已批准的技术基础能力，不构成用户业务接口。2026-09-27批准MsgConfiguration、common.msg.MsgCatalog、agent.web.MsgLocaleResolver/HomePageController作为本地化技术能力；没有新增业务API。
后续用户业务接口、DTO、schema、事务变化在本文同步索引，不能由文档先发明契约。

2026-09-27另行批准ProjectController/Service、ProjectDomain适配、common.memory.MemoryStore、ProjectHttp快照及Prompt/Skill原字段读写。角色映射和规则文件契约详见[项目与执行契约](./project-agent.md#规则绑定)。

2026-09-28用户批准ProjectAgentExecutor、getExecutor(Project)、Long任务主键、Prompt不可变/附件预留、项目名称与四表持久化。ProjectDomain新签名及完整迁移规则见[项目契约](./project-agent.md)；原失效入口删除。mysql未启用时项目503，真实业务调用统一安全预审。
