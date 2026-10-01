# 项目需求与验收

## 当前范围
单 Maven JAR 工程；Java 26、Spring Boot 4.1.1、MyBatis Starter 4.1.0。
用户调用契约位于 user.<业务>.api；共享模型位于 common.agent.model、common.project.model，HTTP 数据位于 common.chat.dto、common.project.dto，ProjectSettings 位于 common.project.config；执行器公共实现位于 agent.runtime。该目录调整保留 user/agent/common 原有职责。
启动入口初始化日志、读取必填项目根目录配置并启动 Spring Web，扫描 user/agent/common，持续运行。
当前有简单首页及单次聊天调用骨架；配置驱动的模型注册、执行器工厂与项目三角色绑定已由 Spring 装配。已补充项目 HTTP 操作、项目工作台、角色默认选择和提示词配置；2026-09-28按用户确认接入项目四表持久化与独立只读安全预审；网页聊天仍未接入，原应用 CLI 已移除。

## 本地与云端开发协作

2026-10-01用户选择本地 push 后通过 GitHub Actions 自动适配 user 包改动。
仅 main 上 user 路径变化自动触发，读取上次成功实现以来的累计 user 净差异；支持手动指定基准处理已批准的既有接口。
五阶段独立 runner：只读规划、独立 Plan Review、受限实现与 Maven clean verify、独立 Result Review、精确快照复核后独立 AI commit 常规回推；禁止依赖未入候选 tree 的忽略配置。
保留 user 原样；不自动变更治理、构建、生产配置/schema 或用户业务决定。审核拒绝、需求不足、越权、验证失败或远端前进即停止，无空提交、无 force/rebase/自动 PR/部署。
工作流和隔离行为验证不等于真实模型/GitHub验收；OPENAI_API_KEY 需由用户在仓库 Actions Secrets 配置，Codex 托管云环境登录不会复用。
详见[协作启用与恢复](docs/operations/cloud-collaboration.md)。

## 用户骨架与最小补充

用户已提供 AgentBean、UserInterface、AgentRegistry、UserService 及项目/执行契约；实现进度见下文，已于2026-09-27完成当前本机Codex与gpt-6-astra的有限项目功能验收，见[验收记录](docs/operations/codex-live-validation.md)。
AgentBean 保存 brand/name/ver、rolePrompt、skill和think，继承DataBean；Skill保存skillName。Prompt为不可变文本及防御复制的byte[]附件预留，构造不做外部调用，当前明确拒绝非空附件。Skill 名称不会触发自动安装或调用。
UserInterface 是由 UserImpl 更名的公共空父接口，用户接口通过 extends 继承；不新增业务方法。
AgentRegistry 位于 user.agent.api，声明列表、brand/name/ver 精确查询与刷新；agent.registry.AgentCatalog 保存配置和本机文件可执行性快照，返回防御性复制的 Bean。UserService 保留原占位行为。
验收包括属性独立读写、边界值、父接口继承实现关系，以及既有 Spring 和真实 Web JAR 回归；测试样例不进入正式 JAR。
早期骨架测试只证明类型契约；实际业务及持久化范围见以下已批准实现，真实模型验收按对应记录区分版本。

## 项目、需求与 Task

Project包含需求和任务，均继承DataBean，保存Long数据库主键、creationData/lastupdateData、delFlg、version。Project保存UUID projectId、可空projectName、三角色AgentBean和projectPrompt，不再保存workingDirectory或执行器引用。Task.id和单次执行/批次UUID严格分离，HTTP Task.id继续为字符串但值为十进制Long。
两阶段规划：先需求，再为信息充分的各需求拆任务；全部成功才加入项目并保存。待确认需求保留空任务，已有任务不得覆盖。ProjectDomain使用newProject(content, projectName[,三角色])、createRequiremensOnNew(Project,content)、createRequiremens、createRequirementsTask、getProject、addProjectPrompt、execTask，以用户最新签名为准。
AgentExecutorFactory.getExecutor(Project)提供唯一项目协调器。execAllTasks(callback)返回批次UUID并最终回调一次，各Task独立执行UUID；execTasks(Long taskId,callback)执行特定本项目PENDING任务。按顺序执行，跳过SUCCEEDED，遇到失败/待确认/RUNNING停止，无自动重试。受理失败同步拒绝且不回调，受理后最终回调；成功、失败、确认字段保持既有语义。
完整Prompt构造后、清旧结果前快照上下文；保存RUNNING成功才审核和调用业务模型，保存终态成功才进入下一任务。模型副作用不在SQL事务内，保存不确定需重载并人工核对，不得自动重跑。详见[项目契约](docs/contracts/project-agent.md)。

## 项目当前结构与变更履历骨架

2026-09-30用户批准先提供实体和抽象Domain供审阅：Project → ProjectApplication → ProjectModule → ProjectFunction；
应用区分后台、前台、App、API服务，同名跨端功能独立，模块只设一层，功能细化到新增/修改订单等行为。
当前态与需求计划分离；RequirementChangePlanItem描述修改范围、不变行为和验收，
TaskChangePlanLink关联任务，ProjectChangeRecord关联实际修改及强类型前后快照。
新外键统一完整类名加Id，均为Long数据库主键；新projectId指Project.id，现有Project.projectId字符串UUID保持原义。

已提供ProjectStructureDomain、RequirementChangeDomain、ProjectChangeDomain三个独立抽象类；
单任务不能跨模块/应用，应用级变更用专用任务。CREATE/UPDATE、父级规划键解析、
实际首次创建绑定、版本校验和幂等发布均仅声明后续实现约束。
本轮没有新DAO、表、SQL、具体实现、Spring装配、HTTP、页面或规划/执行接入；当前系统不会自动维护功能或履历。
验收仅覆盖快照引用列表防御复制/不可修改/可空边界及编译、现有全仓回归。
完整字段与契约见[项目结构骨架](docs/contracts/project-structure.md)，具体实现待用户审阅。

## 模型注册、工厂与项目角色

角色AgentRole.PLANNING/DEVELOPMENT/REVIEW与配置planning/development/review精确映射。config/agents.properties中的zb.agents条目仍显式配置brand/name/ver/type/executable/model/timeout/enabled，当前仅codex；模型三元组精确匹配、重复拒绝，不按品牌推断或静默回退。初始化只检查原生文件，Windows要求.exe，不代表实际模型验证；刷新只重查文件，修改配置需重启。标准属性覆盖JVM > 环境变量 > 文件。
三角色完整用户选择优先于独立显式默认值；任意可用模型可承担各角色，每角色独立快照。工厂按canonical Project身份缓存协调器，UUID/模型/角色规则/skill/think变化拒绝悄悄重绑；数据库元数据不属于模型绑定。think保留推理程度，本轮不自动传入CLI。
需求/任务规划使用planningAgent，开发使用developmentAgent；reviewAgent仅保留业务审核角色，不自动增加代码审核流程。所有真实业务调用另外用各自已配置模型独立只读安全检查，拒绝、错误、超时、非法结构均不放行。Prompt不可变，许可绑定最终输入/客户端/目录/schema/运行模式且只能消费一次，审核后复查关闭/中断/目录。构造不隐藏外部调用；附件非空当前明确拒绝。
保持结构化command+args+stdin、Codex JSONL/schema、read-only审核/规划、workspace-write开发，不改变本机认证或扩大沙箱权限。

## Agent 资源与验证

工厂共享ExecutionResources，规划、拆分、任务与各自审核共用4个票据，无队列。审核和其业务调用顺序占同一票据，各自有单次timeout。回调前归还资源；关闭停止受理、中断工作，沿用单一20秒收尾预算。工厂拥有关闭责任，协调器不单独关闭。
stdout上限8MiB；单次和批次诊断限制64KiB且脱敏标记截断；完成记录最多256条/30分钟，运行中不淘汰，其他协调器/未知/过期ID返回null。批次用量含审核及业务，任何未知值则为null。
验证覆盖真实Java fixture进程、审核阻断/单次许可、两阶段规划、单任务/批次、资源关闭、数据库保存失败隔离和HTTP/JAR。mysql-it用随机前缀四表执行正式DDL、Mapper及DAO，覆盖重载、生成ID、乐观冲突和事务回滚。fixture不等于真实模型能力验收；2026-09-27记录仅代表旧版本有限真实模型链路。

## 项目根目录配置

Spring 启动时读取 config/project.properties 中的 zb.project.root；支持 ZB_PROJECT_ROOT 环境变量及 JVM -Dzb.project.root 覆盖，无默认值。
必须显式配置；缺失、空白、格式非法或已存在但不是目录时启动失败退出1。允许目录尚不存在；相对路径按启动工作目录解析并规范化为绝对路径，不创建目录。
文件入口相对启动工作目录；示例 config/project.properties.example 入仓，真实配置忽略。标准覆盖顺序 JVM 属性 > 环境变量 > 配置文件。
ProjectConfiguration 在根包初始化只读 ProjectSettings；ProjectDomainImpl.newProject在业务调用时创建root/local/projectId，local暂代用户ID；实际路径从配置及规范UUID推导，不存Bean、不接受客户端指定。创建和执行拒绝local/项目目录被链接重定向。进入持久化后失败保留目录；更早失败只尝试删除本次新建空目录。
验收覆盖配置加载与覆盖、错误路径、路径规范化、初始化不创建目录，以及现有 Web/JAR 回归。

## Web
显式提供项目根目录后无参数启动服务，默认 127.0.0.1:8080；远程仅通过 SSH 隧道访问，不开放公网。
首页 /、/index.html 及固定资源 /app.css、/favicon.svg、/chat.js 接受 GET/HEAD；/api/chat 仅接受 POST。项目五个精确入口见[项目与执行契约](docs/contracts/project-agent.md#项目-http-入口)，其余路径不放行。
未批准的路径/方法继续拒绝；聊天错误响应为固定文字，不暴露异常或原始输入。
stdout 不输出业务结果，stderr 为错误及运行诊断，异常保留脱敏后的完整调用链。
启动失败退出1、命令参数错误退出2；正常运行不主动退出，正常停止有20秒优雅关闭窗口。
正式产物 target/zbagentwf-web-0.1.0-SNAPSHOT.jar；不承诺 Maven 类库兼容。

## 单次聊天骨架
agent.chat.controller.ChatController 接收请求；user.chat.service.ChatService 继承 UserService，调用 AgentChat（继承 UserInterface）；agent.chat.AgentChatImpl 仅实现 AgentChat，不继承 AgentRegistry。聊天仍为未接入占位，不调用新的执行契约。
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

显式mysql profile建立数据源；未启用仍能启动Web，项目操作503，无生产内存降级。启用后连接配置和连接失败使启动失败；应用不自动建表或迁移。
用户已批准四表：zb_agent角色快照、zb_project、zb_requirement、zb_task；DDL见src/main/resources/db/project-schema.sql，父子外键及顺序约束、DataBean公共元数据、任务结果JSON。ProjectDao在REQUIRES_NEW短事务保存完整聚合，按行version乐观锁；ID/version只在提交后发布，异常隔离对象直至数据库重载。完整读取REPEATABLE_READ事务，canonical缓存只保证本进程身份；重启后可按ID读取数据库，不迁移旧内存项目。
Hikari最多5连接、最小0、连接5秒/驱动读30秒。mysql-it只接受回环zbagentwf_test，可通过已验证SSH隧道连接专用远程库；随机前缀表测试只清理本次成功创建的表。显式缺配置失败，普通构建跳过环境测试。

## 待用户提供

真实模型新审核/持久化链路及复杂任务、失败/确认/并发验收；reviewAgent业务审核流程、多模态附件支持；正式身份与权限模型、生产部署、会话及旧内存数据迁移策略；日志自动保留策略。当前仅批准专用远程测试库与测试服务器。

## 内存项目、角色默认值与提示词

MemoryStore仍为通用临时引用工具，不持久化、无TTL；项目已迁移到ProjectDao，不再在MemoryStore中登记。规范UUID通过数据库完整查询并发布到唯一canonical对象；数据库不可用不返回旧缓存。详情见[项目契约](docs/contracts/project-agent.md)。
五类UTF-8规则在启动时从config/prompts读取，缺失/空白/占位在实际使用前拒绝，编码/读取错误启动失败；构造和getter无文件/模型副作用。规则按安全/通用/角色/用户/项目分段，受理任务保留规则快照。planning规则需支持需求和任务两个阶段各自schema。Skill只保存名称。
ProjectController → ProjectService → ProjectDomain；请求仅ID/文本及创建时可选名称/三角色选择，响应安全快照。Controller不持Project锁等待异步worker；所有项目响应在同一canonical对象锁内重载数据库并复制，重载失败不返回旧快照。400/404/503/500、三语固定错误、PROJECT日志脱敏与HTTP安全规则保持。

## 项目工作台

2026-09-27用户批准新增项目页面与相关按钮。GET/HEAD /projects、/projects.html 提供项目工作台，首页提供入口。页面增加可选项目名称，复用已有创建、按ID查询/刷新、追加提示词、追加需求、执行待办任务五项HTTP接口；创建使用服务端默认三角色或完整显式的brand/name/ver。不存在全量项目列表、删除、重置、取消或自动审核按钮，不扩展业务契约。
展示需求原文、理解、验收、待确认内容和任务状态/结果，所有业务内容按纯文本输出；项目保存数据库，重启后可按ID打开；页面本身不持久化业务数据。单页请求单飞，长操作不自动超时重发；已知项目写入的503/500/连接或格式错误保留旧快照，成功查询前锁定写入，避免把部分执行当作完全未执行。200响应按Task实际状态展示。
共用原三语Cookie及安全模板机制，语言切换保留表单/结果/进行中调用。新增静态资源为messages.js、projects.js、projects.css，精确路由白名单与原CSP不变。验收覆盖三语模板、路由/HEAD、安全文本、五按钮、模型选择、等待/错误恢复、窄屏、聊天回归及真实JAR；浏览器替身不等于真实模型验收。
