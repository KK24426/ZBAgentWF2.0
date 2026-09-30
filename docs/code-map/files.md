# 文件索引

单工程：根pom负责构建，根REQUIREMENTS记录验收。以下列出Java源码及本地化相关资源。

| 文件 | 类型 |
| --- | --- |
| `src/main/java/com/kk24426/zbagentwf/MsgConfiguration.java` | 显式加载消息/模板并装配语言解析 |
| `src/main/java/com/kk24426/zbagentwf/common/msg/MsgCatalog.java` | 三语校验、外部覆盖及不可变纯文本消息查询 |
| `src/main/java/com/kk24426/zbagentwf/agent/web/MsgLocaleResolver.java` | Cookie/配置/请求头优先级及请求级语言缓存 |
| `src/main/java/com/kk24426/zbagentwf/agent/web/HomePageController.java` | 原首页路径的安全消息模板渲染 |
| `src/main/resources/web/index.html` | 非公开首页模板与语言选择 |
| `src/main/resources/msg/msg_zh_CN.properties`、`msg_en.properties`、`msg_ja.properties` | 内置UTF-8三语消息 |
| `config/msg.properties.example`、`config/msg/*.properties.example` | 默认语言及部分消息覆盖示例 |
| `src/test/java/com/kk24426/zbagentwf/MsgConfigurationTest.java` | 默认及非法语言配置 |
| `src/test/java/com/kk24426/zbagentwf/common/msg/MsgCatalogTest.java` | 三语、参数、覆盖快照及启动错误 |
| `src/test/java/com/kk24426/zbagentwf/agent/web/MsgLocaleResolverTest.java` | 优先级、请求头容错及并发隔离 |
| `src/test/java/com/kk24426/zbagentwf/agent/web/HomePageControllerTest.java` | 首屏、模板约束、恶意译文转义 |
| `src/test/browser/localization_fixture.py` | 仅回环浏览器验收替身，代理真实页面并模拟延迟/超时/错误，不进入JAR |
| `src/main/java/com/kk24426/zbagentwf/ZbAgentWfApplication.java` | Web启动与生命周期 |
| `src/main/java/com/kk24426/zbagentwf/ProjectConfiguration.java` | 必填项目根目录读取与初始化校验，不创建目录 |
| `src/main/java/com/kk24426/zbagentwf/agent/web/WebRequestFilter.java` | 请求保护与安全日志 |
| `src/main/java/com/kk24426/zbagentwf/agent/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/MySqlConfiguration.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/SqlDiagnosticsInterceptor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/agent/persistence/mapper/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/LogFailureMonitor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/RunLogging.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/SanitizingEncoder.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/logging/SecretRedactor.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/common/agent/model/AgentBean.java` | Agent 数据骨架及属性访问 |
| `src/main/java/com/kk24426/zbagentwf/common/agent/model/AgentExecutionResult.java` | 单次执行结果及待确认内容 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/Project.java` | 数据库/业务标识、名称、三角色快照和需求列表 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/Requirement.java` | 项目内需求及Task列表 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/RequirementTask.java` | 规划任务、状态与单次执行结果 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/TaskStatus.java` | 已批准的任务状态值，不实现状态转换 |
| `src/main/java/com/kk24426/zbagentwf/common/project/config/ProjectSettings.java` | 初始化后的只读项目根目录 |
| `src/main/java/com/kk24426/zbagentwf/user/package-info.java` | 运行代码 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/api/AgentRegistry.java` | 本机Agent发现、查询和刷新抽象契约 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/api/ProjectAgentExecutor.java` | 项目协调器、批次和Long单任务执行契约 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/api/AgentExecutionCallback.java` | 执行最终结果回调契约 |
| `src/main/java/com/kk24426/zbagentwf/user/project/api/ProjectDomain.java` | 项目创建、需求规划和任务结果汇总契约 |
| `src/main/java/com/kk24426/zbagentwf/user/UserInterface.java` | 用户接口的空父接口 |
| `src/main/java/com/kk24426/zbagentwf/user/UserService.java` | 用户服务父类占位，未定义行为 |
| `src/main/java/com/kk24426/zbagentwf/agent/chat/controller/ChatController.java` | 聊天HTTP入口与固定错误映射 |
| `src/main/java/com/kk24426/zbagentwf/user/chat/service/ChatService.java` | 用户侧请求校验与Agent调用编排 |
| `src/main/java/com/kk24426/zbagentwf/user/chat/api/AgentChat.java` | 单次Agent调用公共接口 |
| `src/main/java/com/kk24426/zbagentwf/common/exception/AgentUnavailableException.java` | Agent实现与调用层共用的不可用异常 |
| `src/main/java/com/kk24426/zbagentwf/agent/chat/AgentChatImpl.java` | 正式Agent占位实现，明确不可用 |
| `src/main/java/com/kk24426/zbagentwf/common/chat/dto/ChatRequest.java` | 聊天请求与字段类型绑定 |
| `src/main/java/com/kk24426/zbagentwf/common/chat/dto/ChatResponse.java` | 聊天回复数据载体 |
| `src/test/java/com/kk24426/zbagentwf/user/chat/api/ChatAgentFixture.java` | 可控Agent测试替身，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/user/chat/service/ChatServiceTest.java` | 输入边界、原样委派、生产占位测试 |
| `src/test/java/com/kk24426/zbagentwf/agent/chat/controller/ChatControllerTest.java` | Controller/Service/替身HTTP与隐私失败路径测试 |
| `src/test/java/com/kk24426/zbagentwf/agent/web/WebRequestFilterTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/WebJarIT.java` | 真实Web JAR测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/ApplicationContextTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/MySqlIT.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/InjectionImplementation.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/persistence/mapper/ProbeMapper.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/common/logging/LoggingTest.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/user/InjectionFixture.java` | 测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/common/agent/model/AgentBeanTest.java` | 属性契约测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/user/UserInterfaceTest.java` | 父接口关系测试，不进入JAR |
| `src/test/java/com/kk24426/zbagentwf/common/project/model/ProjectModelTest.java` | 多层包含、独立列表与属性契约 |
| `src/test/java/com/kk24426/zbagentwf/user/agent/api/AgentExecutionContractTest.java` | 测试替身的项目传递、执行标识及回调关联 |
| `src/test/java/com/kk24426/zbagentwf/ProjectConfigurationTest.java` | 必填目录、规范化、错误与无建目录副作用 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexAgentExecutor.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexClient.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexJson.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexRequirementPlanner.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/codex/CodexSchemas.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/main/java/com/kk24426/zbagentwf/agent/project/ProjectDomainImpl.java` | Codex 执行、规划或项目具体实现，显式构造组合 |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/CodexAgentExecutorTest.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/CodexFixtureSupport.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/CodexRequirementPlannerTest.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/codex/FakeCodexFixture.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/project/ProjectDomainImplTest.java` | Codex/项目实现验证，测试类不进入 JAR |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentCatalog.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentDefinition.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentExecutorFactoryImpl.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/AgentRequirementPlanner.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/agent/runtime/ExecutionResources.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/AgentConfiguration.java` | 模型注册、工厂装配或共享资源实现 |
| `src/main/java/com/kk24426/zbagentwf/user/agent/api/AgentExecutorFactory.java` | 模型注册、工厂装配或共享资源实现 |
| `src/test/java/com/kk24426/zbagentwf/agent/registry/AgentRegistryTest.java` | 测试，不进入正式JAR |
| `src/test/java/com/kk24426/zbagentwf/agent/runtime/ExecutionResourcesTest.java` | 测试，不进入正式JAR |
| `src/test/java/com/kk24426/zbagentwf/AgentConfigurationTest.java` | 测试，不进入正式JAR |

入口ZbAgentWfApplication负责Web初始化与生命周期；CLI类和测试已移除。
ProjectConfiguration通过application.properties导入启动工作目录下的config/project.properties；config/project.properties.example仅为入仓示例，真实配置不入仓。
首页模板为src/main/resources/web/index.html，由HomePageController在原路径返回；静态资源app.css、favicon.svg和chat.js仍位于static目录。MsgConfiguration导入config/msg.properties并读取config/msg/可选消息覆盖。脚本从首页inert template读取三语消息，只持久化语言Cookie；单次请求结果仍只作为纯文本显示。
agent.persistence负责可选MySQL配置与无参数SQL诊断；common.logging负责日志路径、脱敏、故障可见性。
测试包含聊天调用替身、跨包注入、日志滚动、真实JAR未接入路径、显式MySQL专用库验收。package-info仅职责标记。

规则入口：根AGENTS.md、三个包AGENTS.md、根REQUIREMENTS.md。
流程入口：[开发技能](../../.agents/skills/zb-development/SKILL.md)与[审核技能](../../.agents/skills/zb-review/SKILL.md)；审核模板随审核技能维护。

## 项目内存与规则新增文件

| 文件 | 职责 |
| --- | --- |
| `src/main/java/com/kk24426/zbagentwf/agent/runtime/AbstractAgentExecutor.java` | 保存 Agent 绑定快照并在显式初始化后组合通用与角色规则。 |
| `src/main/java/com/kk24426/zbagentwf/agent/project/controller/ProjectController.java` | 接收项目操作，通过服务查找数据库项目并返回安全快照及本地化错误。 |
| `src/main/java/com/kk24426/zbagentwf/user/project/service/ProjectService.java` | 校验项目输入并通过用户定义的领域接口编排操作。 |
| `src/main/java/com/kk24426/zbagentwf/common/exception/AgentConfigurationUnavailableException.java` | 区分模型或规则尚未配置与执行中的内部错误。 |
| `src/main/java/com/kk24426/zbagentwf/common/memory/MemoryStore.java` | 按分类和标识保存本进程内的对象引用。 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/AgentRole.java` | 项目角色类型或相关验证。 |
| `src/main/java/com/kk24426/zbagentwf/common/project/dto/ProjectHttp.java` | 定义项目 HTTP 输入及不含运行时引用的响应快照。 |
| `src/main/java/com/kk24426/zbagentwf/common/agent/model/AgentReviewResult.java` | 项目角色类型或相关验证。 |
| `src/main/java/com/kk24426/zbagentwf/common/agent/model/Prompt.java` | 保存提示词文本。 |
| `src/main/java/com/kk24426/zbagentwf/common/agent/model/Skill.java` | 保存Skill 名称，不自动加载或执行。 |
| `src/main/java/com/kk24426/zbagentwf/agent/prompt/PromptCatalog.java` | 显式加载 UTF-8 规则快照，查询时不进行文件访问。 |
| `src/main/java/com/kk24426/zbagentwf/agent/registry/RoleAgentResolver.java` | 将任意已选择模型与角色规则绑定，缺省选择由显式配置提供。 |
| `src/test/java/com/kk24426/zbagentwf/agent/runtime/AgentPromptTest.java` | 验证提示词显式初始化、输入快照和缺规则时不启动进程。 |
| `src/test/java/com/kk24426/zbagentwf/agent/project/controller/ProjectControllerTest.java` | 验证项目 Controller、Service、精确路由及三语隐私边界。 |
| `src/test/java/com/kk24426/zbagentwf/common/memory/MemoryStoreTest.java` | 验证共享内存存储的类型、分类、引用和并发边界。 |
| `src/test/java/com/kk24426/zbagentwf/common/project/dto/ProjectHttpTest.java` | 验证项目响应是独立且一致的不可变数据快照。 |
| `src/test/java/com/kk24426/zbagentwf/agent/project/ProjectMemoryTest.java` | 验证项目登记、角色选择、提示词追加及失败不发布。 |
| `config/prompts/*.txt.example` | 五类UTF-8规则占位，实际.txt不入仓 |

## 项目工作台文件

| 文件 | 职责 |
| --- | --- |
| `src/main/java/com/kk24426/zbagentwf/agent/web/LocalizedPageRenderer.java` | 首页与项目页共用的固定消息校验、HTML转义和inert JSON渲染 |
| `src/main/java/com/kk24426/zbagentwf/agent/web/ProjectPageController.java` | 项目页GET/HEAD入口，不实现业务操作 |
| `src/main/resources/web/projects.html` | 非公开项目工作台模板与五项操作表单 |
| `src/main/resources/static/messages.js` | 两页面共用的语言切换及固定文案更新 |
| `src/main/resources/static/projects.js` | 项目快照、单飞请求、状态展示和不确定写入恢复 |
| `src/main/resources/static/projects.css` | 响应式项目布局与任务状态样式 |
| `src/test/java/com/kk24426/zbagentwf/agent/web/ProjectPageControllerTest.java` | 项目三语模板、恶意文案转义与精确路由验证 |
| `src/test/browser/project_fixture.py` | 回环浏览器测试替身，只代理只读页面，业务请求全部本地模拟 |
| `src/test/browser/project_page_test.cjs` | 浏览器行为回归：按钮载荷、错误恢复、语言保留、纯文本与窄屏 |

## 项目持久化与安全审核

| 文件 | 职责 |
| --- | --- |
| src/main/java/com/kk24426/zbagentwf/common/DataBean.java | 用户公共元数据及可访问属性 |
| src/main/java/com/kk24426/zbagentwf/common/DataBeanDao.java | 用户DAO签名，ProjectDao实现完整聚合 |
| src/main/java/com/kk24426/zbagentwf/agent/persistence/ProjectDao.java | 四表读写短事务、版本冲突、提交后发布、不确定状态隔离 |
| src/main/java/com/kk24426/zbagentwf/agent/persistence/mapper/ProjectMapper.java | 参数绑定技术行入口 |
| src/main/resources/mapper/ProjectMapper.xml | 四表生产SQL与父子/版本约束 |
| src/main/resources/db/project-schema.sql | 显式部署的四表DDL，应用不自动执行 |
| src/test/java/com/kk24426/zbagentwf/ProjectPersistenceIT.java | 生产DDL/Mapper/DAO真实MySQL隔离事务验收 |
| src/test/java/com/kk24426/zbagentwf/agent/persistence/ProjectDaoTest.java | 正式DAO可用性探测失败隔离与跨批次共享实体拒绝 |
| src/test/java/com/kk24426/zbagentwf/agent/codex/CodexSafetyTest.java | 单次许可、客户端归属、中断和附件拒绝 |
| src/test/java/com/kk24426/zbagentwf/agent/registry/ProjectFixtureSupport.java | 测试专用深拷贝Store和Java模型进程，不进入正式JAR |

原ProjectMemoryTest迁移为数据库重载和canonical身份测试，CodexAgentExecutorTest/PlannerTest及直接调用者适配新接口。Prompt、CodexClient、ProjectDomainImpl、ProjectSettings及工厂仍在原职责包；没有新增模块。

## 项目结构与变更履历骨架

2026-09-30仅新增类型和抽象Domain；[字段与契约](../contracts/project-structure.md)区分当前态、规划意图和实际履历。
尚未接入数据库、页面或Agent规划/执行；既有ProjectDomain和DataBeanDao不变。

| 文件 | 职责 |
| --- | --- |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectApplication.java` | 项目中应用端的当前名称和说明；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectModule.java` | 一个应用内单层模块的当前名称和说明；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectFunction.java` | 一个模块内具体功能的当前摘要、详细行为和实现引用；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/RequirementChangePlanItem.java` | 需求预计修改的对象、归属路径与修改边界；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/TaskChangePlanLink.java` | 需求任务与负责计划项的关联；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectChangeRecord.java` | 需求/任务/计划项对应的实际修改与前后快照；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectElementType.java` | 应用、模块和具体功能三种对象层级；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectChangeOperation.java` | 创建与修改两种操作；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectElementSnapshot.java` | 应用/模块/功能三种不可变强类型快照；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/common/project/model/ProjectChangeSubmission.java` | 待发布的单项实际修改候选；无业务持久化实现 |
| `src/main/java/com/kk24426/zbagentwf/user/project/api/ProjectStructureDomain.java` | 按项目查询当前应用、模块和功能，仅抽象契约 |
| `src/main/java/com/kk24426/zbagentwf/user/project/api/RequirementChangeDomain.java` | 新增需求计划、配置与查询任务修改范围，仅抽象契约 |
| `src/main/java/com/kk24426/zbagentwf/user/project/api/ProjectChangeDomain.java` | 发布实际修改和按对象/需求/任务查询履历，仅抽象契约 |
| `src/test/java/com/kk24426/zbagentwf/common/project/model/ProjectElementSnapshotTest.java` | 快照隔离、不可修改及引用列表边界测试，不进入 JAR |
