# 项目、需求与 Agent 执行契约

用户于 2026-09-25 批准本轮数据结构、方法签名及配置初始化。随后用户批准先接入 Codex 及项目操作规则；2026-09-26进一步批准配置注册、Spring工厂、项目角色绑定和资源限制，当时将真实模型验收延后；2026-09-27按用户要求完成有限项目功能验收，见[验收记录](../operations/codex-live-validation.md)。

## 包含关系与数据

Project → List<Requirement> → List<RequirementTask>，均是独立 Java 类，位于 common.project.model。没有父对象反向引用、持久化字段或外键。

| 类型 | 字段 |
| --- | --- |
| Project | String projectId、Path workingDirectory、List<Requirement> requirements；AgentExecutor planningAgent/developmentAgent/reviewAgent；Prompt projectPrompt |
| Requirement | String userContent、agentUnderstanding、acceptanceCriteria、userConfirmMsg；List<RequirementTask> tasks |
| RequirementTask | String id、content、acceptanceCriteria；TaskStatus status；AgentExecutionResult result |
| common.agent.model.AgentExecutionResult | String taskId、errorMessage、summary、confirmationMessage；boolean success、confirmationRequired；Long tokenCount |

所有 Bean 提供无参构造与标准 getter/setter，属性原样存取，不自动校验或推断业务状态。requirements/tasks 各自默认 new ArrayList，每个实例独立；setter 原样接收传入列表（包括 null），不复制、不自动建立父子绑定。其它引用默认 null，boolean 默认 false；Task 默认 PENDING。

TaskStatus：PENDING、RUNNING、SUCCEEDED、FAILED、NEEDS_CONFIRMATION。普通 Bean 不自动转换状态；项目实现按下述规则更新。
RequirementTask.id 是规划任务标识，AgentExecutionResult.taskId 是一次执行标识。tokenCount 为 null 表示未知，不等同于零。

## 用户接口

user.project.api.ProjectDomain implements UserInterface：

- Project newProject(String content, AgentBean planningAgent, AgentBean developmentAgent, AgentBean reviewAgent)：先解析三个模型，绑定到新项目后建立 UUID 目录并规划首批需求。
- List<Requirement> createRequirements(Project project, String content)：成功后追加需求，返回本次新增需求及 Task。
- List<Requirement> execTask(Project project)：按该项目完整需求列表顺序执行，等待底层结束，返回含 Task 状态和结果的需求列表。旧二参数入口已删除。
- Project newProject(String content)：使用显式配置的三个角色默认模型。
- Project getProject(String projectId)：查询本次运行内登记的原对象；空白/未知ID抛IllegalArgumentException，不自动创建。
- void addProjectPrompt(String projectId, String content)：非空白文本在现有项目规则后换行追加。

项目具体实现规则见下文；包含关系仍通过 Bean 列表表达。

user.agent.api.AgentExecutor implements UserInterface，通过构造函数保存 private final AgentBean，protected getAgent() 供实现类读取；工作目录从本次传入的 Project 获取：

- String exec(Project project, String content, String memory, AgentExecutionCallback callback)：异步受理，返回单次执行标识。memory 没有时可为空。
- String getStderr(String taskId)：读取该次执行的诊断文本；stderr 不决定执行成败，不能原样公开或写日志。
- AgentExecutionCallback.onCompleted(AgentExecutionResult result)：每次受理的执行最终回调一次，result.taskId 与返回值一致；不是过程事件。

提交失败同步抛 RejectedExecutionException，不触发回调；受理后的失败通过结果表达。成功为 success=true、confirmationRequired=false；普通失败两者 false；待确认为 success=false、confirmationRequired=true 并提供确认内容。
待确认意味着本次执行已经结束，调用方获得用户答复后重新提交，取得新的执行标识。本轮没有暂停恢复或取消接口，也不把成功和待确认同时设置为 true。

AgentExecutionContractTest 的手动替身仅说明契约；具体 Codex 实现通过真实 Java 子进程 fixture 验证，另已通过当前本机Codex的有限真实模型验收，范围见上述记录。

## 项目根目录配置

启动工作目录下 config/project.properties 提供 zb.project.root，示例见 [project.properties.example](../../config/project.properties.example)。真实配置被 Git 忽略，不放进 JAR。
无默认根目录；可通过 ZB_PROJECT_ROOT 或 JVM -Dzb.project.root 覆盖，标准优先级 JVM > 环境变量 > 配置文件。空覆盖值同样无效，不回退到低优先级配置。
目录相对于启动工作目录解析，并规范化为绝对路径。缺失、空白、格式非法、已存在非目录或悬空链接使初始化失败，正常入口退出1；不存在的目录允许，但初始化绝不创建它。

根包 ProjectConfiguration 通过 Spring 初始化只读 ProjectSettings，其 getRootDirectory() 供 ProjectDomainImpl 使用；Project.workingDirectory 是单个项目的实际目录，两者不混用。
Java properties 使用标准转义规则；Windows 推荐正斜线，中文也可用 Unicode 转义。文件只保存根目录设置，不保存账号或凭据。

## 兼容与验收

本次更新接口方法及字段名称，不保留旧签名兼容层；UserImplTest 更名 UserInterfaceTest。调用方需迁移 import 和实现声明。
必填根目录改变无配置启动行为：运行 JAR、IDE 和后续部署前必须提供配置；应用仍不接受命令参数。
验证实体多层包含及独立默认列表、接口替身、Spring 配置与实际 JAR 的加载/覆盖/失败退出；正常 Web 页面、聊天503、原有启动失败路径继续回归。MySQL 环境验收仍独立启用。


## 可调用的具体实现

- agent.codex.CodexAgentExecutor：异步执行与诊断查询。
- agent.codex.CodexRequirementPlanner：只读规划辅助。
- agent.codex.CodexClient：二者共用的本机进程适配器。
- agent.project.ProjectDomainImpl：项目目录、需求追加及等待异步结果的串行执行。

Spring装配 AgentCatalog（AgentRegistry）、AgentExecutorFactoryImpl（AgentExecutorFactory）和 ProjectDomainImpl（ProjectDomain）。调用方用 getActiveAgent(brand,name,ver) 与 getExecutor(agent) 获取对应模型；键精确匹配，工厂深拷贝元数据和嵌套规则、按实体身份绑定实例，不从品牌猜CLI，不自动回退。
config/agents.properties 的 zb.agents[条目键] 使用 brand/name/ver/type/executable/model/timeout/enabled，所有字段显式配置，当前仅 codex；type表示执行实现，model为CLI参数，ver不代表CLI版本。示例见[agents.properties.example](../../config/agents.properties.example)。条目键按来源合并字段，JVM > 环境变量 > 文件；无配置允许Web启动，缺必填/重复三元组/非法值启动失败。不可用模型用 AgentConfigurationUnavailableException（IllegalStateException 子类）；无可用模型列表或工厂关闭仍用 IllegalStateException，空/无效三元组用 IllegalArgumentException。
AgentCatalog初始化只检查文件可执行性，Windows只接受.exe，不运行CLI/鉴权；返回Bean副本。刷新只重查已加载配置中的文件状态；删除文件后刷新清除可用项，配置修改需要重启。已经取得的执行器若随后丢失CLI，按普通执行失败返回，不承诺实时可用性。
Project保存三个AgentExecutor运行时引用；user接口类型依赖由用户明确批准，不保存线程/CLI参数/凭据到实体，也不承诺实体序列化或持久化。工厂统一拥有实例生命周期，调用方不能关闭共享实例。AgentRequirementPlanner使用当前工厂中与planningAgent同身份的规划适配器；跨工厂或自定义实例绑定无法规划时明确失败。reviewAgent当前仅绑定，网页聊天仍未接入。

## 执行器与进程细节

每次受理返回 UUID。快速执行可在 exec 返回前完成回调，调用者按 result.taskId 关联，不能假设回调发生在返回之后。工作目录在受理时快照。无效输入、无效目录、全工厂四个执行额度已满或关闭时同步拒绝且不回调；只读规划也消耗同一额度。回调异常只记录脱敏诊断，不重复回调。close 停止受理、中断执行并有限等待；不承诺抵抗 JVM 崩溃或调用方永不返回的回调。

ProcessBuilder 分离 executable、args 和 UTF-8 stdin，固定使用 Codex exec JSONL、临时输出 schema、ephemeral、无颜色和允许普通非 Git 项目。任务 sandbox 为 workspace-write，规划为 read-only，approval 为 never；保留 CLI 原有配置、认证和规则，不使用危险绕过选项。权限或工具错误不能冒充成功，程序不自行扩大权限。
退出码 0、唯一 turn.completed、无 turn.failed/error 且最终 agent_message 为合法结构化数据，才接受协议结果；业务结果仍可失败或待确认。Java 生成执行 ID，usage 的 input_tokens + output_tokens 为 tokenCount，cached_input_tokens 不重复计数；缺失或不可表示时为 null。

三个管道并发处理，整体超时和进程退出后收尾有界，回收主进程及已观察到的后代；ProcessHandle 遍历不等同于操作系统级进程组隔离。stdout 超过 8 MiB 失败，stderr 保留至多 64 KiB 并标明截断、脱敏。schema 临时文件结束后删除；日志隐藏自由文本但保留异常类型、堆栈和关系，单独记录生成标识、固定阶段及退出码。
getStderr 在受理后可查，最终回调前更新，执行中可能为空字符串，未知 ID 返回 null。内容不得直接公开到 HTTP，脱敏无法保证识别任意隐私文本。已完成诊断全工厂最多256条并保留30分钟，按完成顺序淘汰；运行中条目不淘汰，未知、其他执行器所有和过期ID返回null，Task结果不受影响。定时清理及读取时校验到期；关闭清空并禁止晚写回。ContextClosedEvent停止受理并同时中断任务，工厂close等待票据归还，沿用从关闭开始的单一20秒预算，不join长期调用线程，也不按实例累计等待。单任务timeout单独配置；Spring Web的每phase20秒不是整个应用所有资源的总退出承诺。

## 项目具体行为

newProject 完成首次规划后才登记到通用内存表。创建失败不登记。newProject 验证非空内容，先通过工厂解析规划/开发/审核三个模型；解析失败不创建目录。全部成功后绑定实例，在 root/UUID 建目录并使用 planningAgent 规划首批需求。只有业务调用才创建尚不存在的根目录，初始化不创建。普通创建失败抛 IllegalStateException；受理拒绝及配置未就绪保留原异常类型用于503映射。仅尝试删除本次创建且仍为空的项目目录，绝不递归删除；保留根目录和外部写入的文件。
createRequirements 校验项目目录存在且真实路径位于根目录之下，完整规划成功后把列表替换为“旧需求 + 新需求”，原需求对象保留，返回本次新增列表；外部持有的旧列表引用不自动同步。这兼容 Bean setter 接受的不可变列表，失败不追加部分数据。
规划只读分析原始内容和目录，返回理解、验收和按顺序执行的 Task；每条 userContent 保存本次完整输入。Java 生成 Task UUID，默认 PENDING、result=null。信息不足时 userConfirmMsg 为待确认事项且 tasks 为空，不自动发明业务规则。规划失败的受限脱敏 stderr 保留在 cause 的 suppressed 异常中供本地诊断，顶层错误仍固定；不得原样外传或用其它日志组件直接打印这段自由文本。

execTask 开始前按对象身份检查需求归属，拒绝重复需求、共享 Task、无效 Task 标识/状态/内容及目录越界。同一实现实例对同一真实项目目录串行操作，锁的引用计数包含等待者，最后一位离开后回收；调用方不要在执行期间直接并发修改 Bean 或角色绑定。execTask在开始时快照developmentAgent，本轮不自动执行reviewAgent。
按项目需求及 Task 列表顺序，只执行 PENDING。在本次范围内遇到既有 FAILED/NEEDS_CONFIRMATION 也立即返回，不能再次调用就自动绕过；SUCCEEDED 可跳过。提交时先快照上下文再置 RUNNING、清旧结果，受理失败恢复 PENDING 和旧结果并抛 RejectedExecutionException；受理后等待回调，写回结果并映射 SUCCEEDED、FAILED 或 NEEDS_CONFIRMATION。失败或待确认立即返回，后续任务不变。
调用方补充确认相关内容、将目标 Task 重置为 PENDING 后重试，保留规划 id，替换单次结果。上下文包含当前需求原始内容、理解、验收、确认相关内容，以及按规划任务和旧执行 ID 关联的先前摘要、确认问题、失败原因，在清旧结果前生成；简短答复也能关联原问题。不跨项目共享记忆。等待线程中断后仍等待已受理执行完成，再保留中断标志返回，不启动下一项，没有新增取消协议。

## 实现验证边界

真实 Java fixture 子进程验证协议、中文 stdin/cwd、并发管道、非零退出、错误结果、确认、超时、回调一次、关闭及后代管道收尾；项目测试验证创建、追加、串行、停止、人工重试和数据隔离。fixture 不进入正式 JAR，完整 verify 继续覆盖 Web/JAR。2026-09-27已验证本机已有登录、所选gpt-6-astra的实际调用及两文件项目成功链路；真实复杂任务、失败/确认和并发场景仍未验收，没有新增持久化、业务 Git 操作或部署；项目 HTTP 入口按下文用户批准范围实现。

## 内存登记及规则绑定

2026-09-27用户批准：ProjectDomain替代原ProjectUserif，新增内存查询/项目规则追加、无参模型选择入口及项目Controller；以本节替代旧“同模型共享实例”的行为。
common.memory.MemoryStore由Spring装配为单例。键是(namespace,id)二元组，值保存Java原引用；put同键替换，get需显式Class且返回Optional，类型错误抛IllegalArgumentException，值及Class不允许null，分类/ID不允许空白。remove只移除引用，不负责停止执行器或删除目录。无持久化、TTL或容量淘汰；不将唯一项目数据当成可丢弃缓存。服务重启后无法只凭ID还原对象。业务对象内部线程安全由调用者负责，不通过返回原引用授予并发修改权限。

zb.agent-roles.<planning/development/review>.brand/name/ver与模型注册分开。任意可用模型能承担任意角色；完整显式选择优先，默认缺失/不完整在使用时抛AgentConfigurationUnavailableException，不选首个可用模型。默认在启动时读取，项目绑定固定，配置修改后重启，仅新建项目使用新默认。未知角色配置名视为配置错误。
每个新项目对三角色分别创建独立AgentBean；角色规则由AgentBean.rolePrompt指定，未指定时从对应角色文件读取。工厂以对象身份缓存，实体/嵌套Prompt文本/Skill名称修改后再次getExecutor会拒绝；已绑定实例保留深拷贝。不同实体即使三元组和规则相同也不会共用执行器。共享4额度、诊断限制和工厂关闭规则不变；实例由工厂持有到应用关闭，不自动回收活动项目或执行器。

PromptCatalog在启动阶段显式读取config/prompts中的default.txt、security.txt、planning.txt、development.txt、review.txt。UTF-8（接受BOM），无热加载；不存在/空白或以YOUR_开头的占位内容视为未配置，访问必需规则时抛AgentConfigurationUnavailableException；现有目录/文件不可读或UTF-8非法时启动失败。只读取固定名称，HTTP不能指定配置路径；真实规则被Git忽略，仓库只放.txt.example。
AgentExecutor构造器只保存字段，getUserPrompt/getProjectPrompt为protected。AbstractAgentExecutor提供显式initializePrompts(default,security)，保存文本快照且拒绝重复初始化；Codex执行器和独立规划器未初始化时均在进程启动前拒绝。规则按安全、通用、角色、用户、项目分段传入stdin，用户/项目规则可为空；项目对象的最新projectPrompt优先于构造时的项目规则。规划/执行在调用受理前生成快照，追加只影响后续调用，不改变进行中工作。分段文本不是操作系统权限保障，原sandbox与应用校验继续有效。
Skill只保存skillName，不读取Skill文件，不自动安装或触发Skill执行。

## 项目 HTTP 入口

ProjectController 位于 agent.project.controller，通过 user.project.service.ProjectService 调用用户定义的 ProjectDomain。2026-09-27按用户要求从 user.project.controller 迁入；Java调用方须更新import，旧包不提供兼容类，HTTP契约不变。

Java 请求类型为 ProjectHttp.CreateProjectRequest、ProjectHttp.ProjectContentRequest，响应快照为 ProjectHttp.ProjectResponse；仅类型名称调整，JSON 字段与下表保持一致。

| 方法和路径 | JSON输入 | 成功响应 |
| --- | --- | --- |
| POST /api/projects | content；可选完整planningAgent/developmentAgent/reviewAgent三项，各仅brand/name/ver | 201 项目快照 |
| GET/HEAD /api/projects/{projectId} | 无 | 200 项目快照；HEAD无正文 |
| POST /api/projects/{projectId}/prompts | content | 204，无正文 |
| POST /api/projects/{projectId}/requirements | content | 200 更新后项目快照 |
| POST /api/projects/{projectId}/tasks/execute | 空对象 {} | 200 执行结束后的项目快照 |

文本必须非空白，三个模型全给或全省略，不把部分null暗示为按角色补齐。未知项目404，不从请求反序列化Project，不接受目录、执行器或运行时规则对象。读取与域内修改按同一Project对象锁协调，快照复制后序列化；同项目同步执行期间查询/追加会等待，未引入进度流或取消功能。响应只含projectId和requirements，需求保留userContent/agentUnderstanding/acceptanceCriteria/userConfirmMsg/tasks，Task保留id/content/acceptanceCriteria/status/result，result沿用执行结果字段；不包含工作目录、执行器、模型配置、Prompt或stderr。模型生成的正文保持为业务文本，不等同于内部对象序列化。
输入/JSON错误400；未知ID404；配置/模型未就绪和受理额度拒绝503；内部错误500。内部IllegalArgumentException不一概映射400。错误使用http.project.invalid/notFound/unavailable及既有http.internalError，通过MsgCatalog返回固定三语纯文本。GET的HEAD错误也无正文。Filter仅放行上述精确路由/方法，保留CSP、无缓存、请求编号和默认回环监听。日志使用PROJECT固定类别，不记录URL、ID、正文；项目异常及请求中的Spring诊断隐藏自由文本并保留完整类型、栈、cause和suppressed。
没有新增登录/权限模型、后台批次或真实数据库；项目接口已完成上述有限真实模型验收；原聊天页面和/api/chat继续独立保持原行为。

## 项目页面

2026-09-27用户批准 /projects 和 /projects.html 的项目工作台，首页提供导航入口。按钮直接消费上述五项HTTP契约，创建可使用默认模型或填写完整三角色标识。页面仅按ID打开单个项目，不提供后端尚无的项目列表、删除、任务重置、取消或自动审核功能。
项目与结果仅在服务本次运行中存在；页面不保存业务数据，项目ID不是重启恢复凭据。提示词追加成功只显示回执，不返回或展示服务器保存的提示词。执行结果通过完整快照显示，失败/待确认不能因HTTP200被标为全部成功。
单页面操作串行，请求期间禁用项目控制，语言切换不会丢失输入或重发调用。长操作保留等待，不以固定30秒中断。服务503可能发生在部分Task成功之后；已知项目写请求503/500/网络或结构异常均保留旧快照并锁住写按钮，成功GET后解锁。不自动重试POST；创建结果不明时明确提示核对日志，避免盲目重复创建。
