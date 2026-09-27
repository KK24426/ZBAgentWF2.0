# 项目需求与验收

## 当前范围
单 Maven JAR 工程；Java 26、Spring Boot 4.1.1、MyBatis Starter 4.1.0。
启动入口初始化日志、读取必填项目根目录配置并启动 Spring Web，扫描 user/agent/common，持续运行。
当前有简单首页及单次聊天调用骨架；配置驱动的模型注册、执行器工厂与项目三角色绑定已由 Spring 装配。已补充项目 HTTP 操作、内存登记、角色默认选择和提示词配置；网页聊天仍未接入；业务表和持久化未实现，原应用 CLI 已移除。

## 用户骨架与最小补充

用户已提供 AgentBean、UserInterface、AgentRegistry、UserService 及项目/执行契约；实现进度见下文，真实模型账号验收仍未运行。
AgentBean 保存 brand/name/ver、rolePrompt 和 skill，Prompt 保存 prompt 文本、Skill 保存 skillName；均为普通 Bean，原字段原样存取、默认 null。Skill 名称不会触发自动安装或调用。
UserInterface 是由 UserImpl 更名的公共空父接口，用户接口通过 extends 继承；不新增业务方法。
AgentRegistry 位于 user.agent.userif，声明列表、brand/name/ver 精确查询与刷新；agent.registry.AgentCatalog 保存配置和本机文件可执行性快照，返回防御性复制的 Bean。UserService 保留原占位行为。
验收包括属性独立读写、边界值、父接口继承实现关系，以及既有 Spring 和真实 Web JAR 回归；测试样例不进入正式 JAR。
此最小补充仅用于验证协作流程，不代表复杂业务接口、模型调用或数据库持久化已实现或验收。

## 项目、需求与 Task

Project 包含多条 Requirement，每条 Requirement 包含多个 RequirementTask，均为独立普通 Java Bean；无父对象反向引用、数据库外键或自动业务校验。
Project 保存 projectId、Path workingDirectory、requirements，projectPrompt，以及 planningAgent/developmentAgent/reviewAgent 三个 AgentExecutor 运行时引用；Requirement 保存 userContent、agentUnderstanding、acceptanceCriteria、tasks、userConfirmMsg。
两个列表默认各实例独立的空列表，setter 原样赋值。RequirementTask 保存 id、content、acceptanceCriteria、status、result；默认 PENDING。
TaskStatus 为 PENDING、RUNNING、SUCCEEDED、FAILED、NEEDS_CONFIRMATION，普通 Bean 不自动转换状态；项目实现按下述执行规则更新。
规划任务 id 与单次执行 taskId 区分；AgentExecutionResult 保存 taskId、success、errorMessage、Long tokenCount、summary、confirmationRequired、String confirmationMessage。token 数未知时 null。
AgentExecutor 保存 final 模型引用并提供 protected getter；exec(Project, content, memory, callback) 异步受理后返回 String 执行标识，最终 onCompleted 回调一次；提交失败同步抛 RejectedExecutionException 且不回调。getStderr(taskId) 按执行读取诊断。
成功 success=true、confirmationRequired=false；普通失败两者 false；需要确认 success=false、confirmationRequired=true，本次执行结束，答复后重新提交取得新标识。不增加暂停恢复或取消接口。
ProjectDomain 的 newProject(content, planningAgent, developmentAgent, reviewAgent) 接受显式选择，newProject(content) 使用三角色默认配置。getProject(projectId) 取回本次运行内原对象，addProjectPrompt(projectId, content) 追加规则。createRequirements(project, content) 返回新增需求，execTask(project) 按项目完整需求顺序执行并等待结果；旧二参数入口已删除。
用户进一步批准先接入 Codex：newProject 使用 UUID，目录为 root/UUID，创建后规划首批需求；createRequirements 完整规划成功后追加，返回本次新增列表。execTask 按项目需求和 Task 顺序串行，只执行 PENDING；遇到新产生或既有的失败/待确认 Task 都立即返回，其余 PENDING 保持不变。重试由调用者补充确认相关内容并手动重置 PENDING，新执行标识替换单次结果，规划 id 不变；提交前快照旧确认问题、失败原因及摘要，以便新会话理解答复。

## 模型注册、工厂与项目角色

Java 角色类型使用 AgentRole.PLANNING/DEVELOPMENT/REVIEW；configKey()/fromConfigKey() 精确关联原有 planning/development/review 配置及提示词名称，不接受大写、空白或未知配置键。类型与方法命名按 ADR0013 统一，HTTP/JSON 字段及业务行为保持不变。

config/agents.properties 通过 Spring 启动导入，真实文件忽略、占位示例入仓。zb.agents[条目键] 包含 brand/name/ver/type/executable/model/timeout/enabled，字段全部显式配置；仅 type=codex 已实现。模型三元组精确匹配、必填、重复拒绝，普通 AgentBean 的原样存取契约不变。CLI路径、实际模型参数和单次超时独立于模型元数据。
条目按键合并配置来源，支持 JVM > 环境变量 > 文件的字段覆盖；配置错误导致启动失败。无配置或空表保留 Web 启动能力，查询/刷新无可用项、未知/禁用模型明确失败，不自动挑选替代模型。角色默认值必须单独显式配置。初始化只检查配置文件路径是否为可执行普通文件，Windows要求.exe，不执行CLI或鉴权；“可用”不等于账号模型已验收。refreshAgentList 重新检查已加载配置的文件状态，配置修改后重启。
AgentExecutorFactoryImpl 按 AgentBean 对象身份绑定 CodexAgentExecutor，同一未修改实体复用、不同实体独立；深拷贝元数据/角色提示词/Skill 名称，原实体修改后再次获取明确拒绝。工厂负责生命周期。AgentRequirementPlanner 将项目 planningAgent 与同工厂的只读规划适配器关联，业务调用方无需依赖 Codex 类；手工替换为其它工厂/自定义规划实例会明确失败。
newProject 三个模型全部解析成功后才创建 root/UUID，绑定规划/开发/审核执行器并规划首批需求；createRequirements 使用 planningAgent，execTask 使用 developmentAgent；reviewAgent 仅绑定，不自动增加审核流程。任意可用模型可承担任意角色，但不同项目/角色使用独立 Agent 实体和执行器绑定；上下文分别归属项目。Project 的执行器引用不承诺序列化或持久化，不自行关闭共享实例。
执行继续采用结构化 command/args/stdin、Codex JSONL及输出schema；规划read-only，开发workspace-write，不管理CLI登录或改变已有规则。项目路径归属、完整规划后追加、失败停止和人工重试规则不变；目录锁计入持有者和等待者，最后一位离开后回收。

## Agent 资源与验证

Spring工厂共享ExecutionResources，规划和开发共用4个票据、不排队；异步执行满额或关闭同步抛RejectedExecutionException且不回调，受理后最终回调一次。同步规划也消耗额度；回调前归还票据，支持回调重入关闭。
诊断是按执行ID查询的stderr，不是Task结果。单次stderr至多64KiB并脱敏、标明截断；stdout上限8MiB。全工厂完成诊断最多256条、保留30分钟，先淘汰最早完成记录，运行中条目不淘汰；未知、其他执行器所有或过期ID返回null。定时清理与读时校验共同管理到期，不清除Task状态/结果/摘要。同步规划没有公开执行ID，失败仍通过既有安全异常链保留受限诊断。
ContextClosedEvent停止工厂和票据受理、同时中断执行与规划；销毁阶段等待票据归还，总等待沿用开始关闭时的单一20秒预算，不等待同步调用线程结束。此预算只约束Agent资源，既有Spring Web按phase配置20秒，不承诺全应用所有Bean总退出时长。单任务timeout独立按模型条目配置。关闭后诊断清空且不接收迟到写入；直接构造的低层实例拥有独立资源作用域，应由调用方close。
验证注册/配置覆盖/刷新、工厂版本与复用、Spring接口注入、项目三角色和隔离、共享额度、关闭竞态、容量/TTL、锁回收，并运行真实Java fixture子进程和完整verify。没有访问真实模型账号；用户决定初期功能完成后另行验收。MySQL仍按显式环境测试单独启用。

## 项目根目录配置

Spring 启动时读取 config/project.properties 中的 zb.project.root；支持 ZB_PROJECT_ROOT 环境变量及 JVM -Dzb.project.root 覆盖，无默认值。
必须显式配置；缺失、空白、格式非法或已存在但不是目录时启动失败退出1。允许目录尚不存在；相对路径按启动工作目录解析并规范化为绝对路径，不创建目录。
文件入口相对启动工作目录；示例 config/project.properties.example 入仓，真实配置忽略。标准覆盖顺序 JVM 属性 > 环境变量 > 配置文件。
ProjectConfiguration 在根包初始化只读 ProjectSettings；ProjectDomainImpl.newProject 在业务调用时使用此根目录创建项目，项目自己的目录保存到 Project.workingDirectory。
验收覆盖配置加载与覆盖、错误路径、路径规范化、初始化不创建目录，以及现有 Web/JAR 回归。

## Web
显式提供项目根目录后无参数启动服务，默认 127.0.0.1:8080；远程仅通过 SSH 隧道访问，不开放公网。
首页 /、/index.html 及固定资源 /app.css、/favicon.svg、/chat.js 接受 GET/HEAD；/api/chat 仅接受 POST。项目五个精确入口见[项目与执行契约](docs/contracts/project-agent.md#项目-http-入口)，其余路径不放行。
未批准的路径/方法继续拒绝；聊天错误响应为固定文字，不暴露异常或原始输入。
stdout 不输出业务结果，stderr 为错误及运行诊断，异常保留脱敏后的完整调用链。
启动失败退出1、命令参数错误退出2；正常运行不主动退出，正常停止有20秒优雅关闭窗口。
正式产物 target/zbagentwf-web-0.1.0-SNAPSHOT.jar；不承诺 Maven 类库兼容。

## 单次聊天骨架
user.chat.controller.ChatController 接收请求；user.chat.service.ChatService 继承 UserService，调用 AgentChat（继承 UserInterface）；agent.chat.AgentChatImpl 仅实现 AgentChat，不继承 AgentRegistry。聊天仍为未接入占位，不调用新的执行契约。
AgentUnavailableException 位于 common.exception，由 Agent 实现抛出、Controller 捕获并映射为503。
POST /api/chat 接收 application/json 的 message 字符串，非空白、长度不超过4000个Java UTF-16代码单元；有效内容原样传递。成功返回200及JSON reply字符串；输入/JSON错误400、媒体类型不支持415、方法不支持405；未接入503，内部故障500，错误正文不包含输入或异常细节。
正式实现只明确报告 Agent 尚未接入；测试替身仅存在于测试源码，不打入正式JAR。验收包括 Controller → Service → 替身返回的成功链路，以及生产占位的503；不得把替身验证声称为真实模型验收。
首页提供输入框、发送按钮、发送中状态、结果/错误区和请求编号；重复提交被阻止、完成后恢复按钮，返回文本不作为HTML执行。不保存会话、不增加数据库/外部调用/模型配置。
未来 provider、超时/取消、重试、会话与存储规则由后续任务定义；当前页面30秒等待上限仅防止浏览器一直等待，不承诺取消服务端工作。

## 页面与接口提示本地化
项目自有页面（含标题、元信息、无障碍文字、输入校验、无脚本提示、聊天过程与请求编号）及Filter/Controller固定HTTP提示统一通过消息key查询，提供zh-CN/en/ja。用户输入和模型回复保持原文；日志、内部异常、启动诊断、过滤器之前的容器协议错误不在本轮范围内。
common.msg.MsgCatalog在根包MsgConfiguration显式装配时加载UTF-8三语消息；内置msg/msg_zh_CN.properties、msg_en.properties、msg_ja.properties，启动目录下config/msg/同名外部文件按key覆盖内置同语言。缺外部文件/键合法；未知键、空白消息、重复键、编码或格式错误、占位符不一致使启动失败。纯文本参数仅支持从零连续编号的{0}/{1}等，值不解释为HTML或再次替换。配置及译文修改后重启。
config/msg.properties中zb.msg.locale允许auto/zh-CN/en/ja，默认auto；JVM属性 > ZB_MSG_LOCALE > 文件。语言优先级为合法zb.locale Cookie > 显式服务语言 > Accept-Language > 简体中文。请求头按权重、同权重原序匹配中文/英文/日文主语言，排除q=0和无具体语言的通配符；畸形请求头整体按无偏好处理，不改变原HTTP状态。不使用JVM全局默认语言。
首页由agent.web.HomePageController在原路径渲染非公开模板；MsgLocaleResolver与过滤器/MVC共享请求级语言，禁止跨请求串扰。模板值与JSON先转义为HTML文本，三语前端消息通过inert template传递，无新增HTTP路径或可执行内联脚本。前端仍只按状态码展示固定安全错误，保持CSP、HEAD空体和原业务接口/DTO/状态码。
语言选择提供自动/简体中文/English/日本語，即时更新文字及已有校验/请求状态，保留输入、模型回复、请求编号和正在进行的请求。Cookie仅保存语言一年、Path=/、SameSite=Lax，HTTPS添加Secure；自动清除偏好。浏览器禁用Cookie时不能保证刷新后的持久化，不引入账号/服务端会话或业务数据存储。
验收覆盖三语和UTF-8、参数/覆盖/启动失败、配置及语言优先级、请求隔离、HTTP安全错误、HTML转义、模板不可访问、真实JAR与浏览器切换/刷新/自动恢复/超时/处理中切换及英日布局。

## 日志
项目 DEBUG、框架 INFO；UTF-8 文件按每次运行隔离，20MB/日滚动压缩，不自动删除历史。
记录启动、请求、耗时、关闭及完整异常链；runId 标识进程，requestId 标识请求。
只记录白名单方法、固定路由类别、状态码和耗时，不记录原始URL、查询、请求正文、SQL参数或结果集。
容器在过滤器之前的协议诊断使用固定摘要，屏蔽异常原文但保留来源、级别、类型和完整调用链。
CHAT请求异常及该请求中的Spring诊断使用固定摘要，聊天组件异常即使无HTTP上下文也隐藏消息原文；保留类型/完整堆栈/cause/suppressed和请求关联，普通请求完成记录仍包含方法/类别/状态/耗时。
目录不可写或启动日志故障时失败；运行期日志故障可见并使后续请求返回503，正常关闭前刷新。
已知凭据与常见敏感赋值脱敏；不保证自动识别任意自然语言中的秘密，调用方仍不得记录敏感材料。

## 数据库
显式 mysql profile 才建立 MySQL 数据源，默认无数据源。
Hikari 最大5连接、最小空闲0，连接超时5秒、驱动读取超时30秒。
缺配置或无法连接时失败；不建库、建表、不执行 migration。
Mapper 注入、参数绑定、事务基础已提供；真实业务 schema/事务范围待用户定义。
mysql-it 仅接受回环地址 zbagentwf_test 和独立环境变量，可经 SSH 隧道连接已批准的远程专用库，随机测试表生命周期内验证CRUD、中文、提交和回滚。
缺配置时显式测试失败；普通构建跳过该环境测试。

## 待用户提供
真实 Codex 验收、审核 Agent 业务流程；后续业务表结构、事务边界及会话功能；
生产环境与权限模型（当前仅批准远程测试环境、专用测试库和受限账号）；
日志历史保留策略若需要自动清理，由后续任务定义。

## 内存项目、角色默认值与提示词

common.memory.MemoryStore 是应用单例，以分类+ID保存任意非空对象引用，支持 put/get(Class)->Optional/remove；空键或类型错误明确失败，无TTL、自动淘汰或持久化。Project 完整创建成功后登记在 project 分类；未知/空白 ID 抛 IllegalArgumentException，不自动重建；重启后原数据无法恢复。登记后 projectId 不应改变。
zb.agent-roles.<planning/development/review>.brand/name/ver 提供显式默认选择；用户完整选择三个模型时优先使用其选择。默认缺失/不完整时仅创建失败，Web仍可启动。默认配置在启动时快照，新项目绑定后不会自动换模型。
config/prompts/{default,security,planning,development,review}.txt 为 UTF-8 规则，由用户提供。启动显式加载，缺失/空白/占位规则在使用时拒绝；非法UTF8/不可读文件使初始化失败。构造器和getter不读文件，也不调用子类初始化方法。执行器构造完成后显式初始化通用/安全规则，缺规则不能启动子进程；规划与开发都注入分段规则，项目追加规则在下一次调用生效，已受理调用保留快照。Skill本次仅保存名称。具体规则内容不由应用自动生成，也不替代原进程权限限制。
ProjectController -> ProjectService -> ProjectDomain，后续请求仅提交项目ID及文本，不接受工作目录或执行器。响应为项目ID、需求、Task和结果的不可变快照，同项目修改/复制使用对象锁；目录锁继续保护实际目录。错误区分400/404/503/500并通过msg返回三语固定提示；PROJECT异常与Spring绑定日志隐藏自由文本并保留完整调用链。未增加项目页面、数据库、真实模型验收或部署。
验收覆盖通用异类存储、原对象取回、失败不登记、角色任意绑定与显式选择优先、规则缺失/变更/嵌套隔离、并发快照、五个HTTP操作及真实JAR无配置错误路径。
