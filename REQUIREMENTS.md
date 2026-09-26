# 项目需求与验收

## 当前范围
单 Maven JAR 工程；Java 26、Spring Boot 4.1.1、MyBatis Starter 4.1.0。
启动入口初始化日志、读取必填项目根目录配置并启动 Spring Web，扫描 user/agent/common，持续运行。
当前有简单首页及单次聊天调用骨架；配置驱动的模型注册、执行器工厂与项目三角色绑定已由 Spring 装配。网页聊天仍未接入；业务表和持久化未实现，原应用 CLI 已移除。

## 用户骨架与最小补充

用户已提供 AgentBean、UserInterface、AgentBase、UserService 及项目/执行契约；实现进度见下文，真实模型账号验收仍未运行。
AgentBean 保留 brand/name/ver 三个私有 String 属性，提供标准 getter/setter 和无参构造；默认 null，允许 null、空字符串及中文，原样存取、不校验、不设置默认值。
UserInterface 是由 UserImpl 更名的公共空父接口，用户接口通过 extends 继承；不新增业务方法。
AgentBase 位于 user.agent.userif，声明列表、brand/name/ver 精确查询与刷新；agent.registry.AgentCatalog 保存配置和本机文件可执行性快照，返回防御性复制的 Bean。UserService 保留原占位行为。
验收包括属性独立读写、边界值、父接口继承实现关系，以及既有 Spring 和真实 Web JAR 回归；测试样例不进入正式 JAR。
此最小补充仅用于验证协作流程，不代表复杂业务接口、模型调用或数据库持久化已实现或验收。

## 项目、需求与 Task

Project 包含多条 Requirement，每条 Requirement 包含多个 RequirementTask，均为独立普通 Java Bean；无父对象反向引用、数据库外键或自动业务校验。
Project 保存 projectId、Path workingDirectory、requirements，以及 planningAgent/developmentAgent/reviewAgent 三个 AgentExecutor 运行时引用；Requirement 保存 userContent、agentUnderstanding、acceptanceCriteria、tasks、userConfirmMsg。
两个列表默认各实例独立的空列表，setter 原样赋值。RequirementTask 保存 id、content、acceptanceCriteria、status、result；默认 PENDING。
TaskStatus 为 PENDING、RUNNING、SUCCEEDED、FAILED、NEEDS_CONFIRMATION，普通 Bean 不自动转换状态；项目实现按下述执行规则更新。
规划任务 id 与单次执行 taskId 区分；AgentExecResult 保存 taskId、success、errorMessage、Long tokenCount、summary、confirmationRequired、String confirmationMessage。token 数未知时 null。
AgentExecutor 保存 final 模型引用并提供 protected getter；exec(Project, content, memory, callback) 异步受理后返回 String 执行标识，最终 onCompleted 回调一次；提交失败同步抛 RejectedExecutionException 且不回调。getStderr(taskId) 按执行读取诊断。
成功 success=true、confirmationRequired=false；普通失败两者 false；需要确认 success=false、confirmationRequired=true，本次执行结束，答复后重新提交取得新标识。不增加暂停恢复或取消接口。
ProjectUserif 的 newProject(content, planningAgent, developmentAgent, reviewAgent) 接受三个 AgentBean、返回绑定执行器的 Project；旧无模型入口已删除。createRequirements(project, content) 返回需求列表，execTask(project, requirements) 等待底层结束并返回带 Task 状态和结果的需求列表；列表参数选定项目内的执行范围。
用户进一步批准先接入 Codex：newProject 使用 UUID，目录为 root/UUID，创建后规划首批需求；createRequirements 完整规划成功后追加，返回本次新增列表。execTask 按所选需求和 Task 顺序串行，只执行 PENDING；遇到新产生或既有的失败/待确认 Task 都立即返回，其余 PENDING 保持不变。重试由调用者补充确认相关内容并手动重置 PENDING，新执行标识替换单次结果，规划 id 不变；提交前快照旧确认问题、失败原因及摘要，以便新会话理解答复。

## 模型注册、工厂与项目角色

config/agents.properties 通过 Spring 启动导入，真实文件忽略、占位示例入仓。zb.agents[条目键] 包含 brand/name/ver/type/executable/model/timeout/enabled，字段全部显式配置；仅 type=codex 已实现。模型三元组精确匹配、必填、重复拒绝，普通 AgentBean 的原样存取契约不变。CLI路径、实际模型参数和单次超时独立于模型元数据。
条目按键合并配置来源，支持 JVM > 环境变量 > 文件的字段覆盖；配置错误导致启动失败。无配置或空表保留 Web 启动能力，查询/刷新无可用项、未知/禁用模型明确失败，不选择默认模型。初始化只检查配置文件路径是否为可执行普通文件，Windows要求.exe，不执行CLI或鉴权；“可用”不等于账号模型已验收。refreshAgentList 重新检查已加载配置的文件状态，配置修改后重启。
AgentExecFactoryImpl 按不可变三元组复用 CodexAgentExec，防止修改入参 Bean 影响缓存；工厂负责生命周期。AgentRequirementPlanner 将项目 planningAgent 与同工厂的只读规划适配器关联，业务调用方无需依赖 Codex 类；手工替换为其它工厂/自定义规划实例会明确失败。
newProject 三个模型全部解析成功后才创建 root/UUID，绑定规划/开发/审核执行器并规划首批需求；createRequirements 使用 planningAgent，execTask 使用 developmentAgent；reviewAgent 仅绑定，不自动增加审核流程。相同模型可被多个角色和项目复用，需求/任务与上下文仍分别归属项目。Project 的执行器引用不承诺序列化或持久化，不自行关闭共享实例。
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
ProjectConfiguration 在根包初始化只读 ProjectSettings；ProjectUserifImpl.newProject 在业务调用时使用此根目录创建项目，项目自己的目录保存到 Project.workingDirectory。
验收覆盖配置加载与覆盖、错误路径、路径规范化、初始化不创建目录，以及现有 Web/JAR 回归。

## Web
显式提供项目根目录后无参数启动服务，默认 127.0.0.1:8080；远程仅通过 SSH 隧道访问，不开放公网。
固定资源 /、/index.html、/app.css、/favicon.svg、/chat.js 接受 GET/HEAD；/api/chat 仅接受 POST。
未批准的路径/方法继续拒绝；聊天错误响应为固定文字，不暴露异常或原始输入。
stdout 不输出业务结果，stderr 为错误及运行诊断，异常保留脱敏后的完整调用链。
启动失败退出1、命令参数错误退出2；正常运行不主动退出，正常停止有20秒优雅关闭窗口。
正式产物 target/zbagentwf-web-0.1.0-SNAPSHOT.jar；不承诺 Maven 类库兼容。

## 单次聊天骨架
user.chat.controller.ChatController 接收请求；user.chat.service.ChatService 继承 UserService，调用 AgentChat（继承 UserInterface）；agent.chat.AgentChatImpl 仅实现 AgentChat，不继承 AgentBase。聊天仍为未接入占位，不调用新的执行契约。
AgentUnavailableException 位于 common.exception，由 Agent 实现抛出、Controller 捕获并映射为503。
POST /api/chat 接收 application/json 的 message 字符串，非空白、长度不超过4000个Java UTF-16代码单元；有效内容原样传递。成功返回200及JSON reply字符串；输入/JSON错误400、媒体类型不支持415、方法不支持405；未接入503，内部故障500，错误正文不包含输入或异常细节。
正式实现只明确报告 Agent 尚未接入；测试替身仅存在于测试源码，不打入正式JAR。验收包括 Controller → Service → 替身返回的成功链路，以及生产占位的503；不得把替身验证声称为真实模型验收。
首页提供输入框、发送按钮、发送中状态、结果/错误区和请求编号；重复提交被阻止、完成后恢复按钮，返回文本不作为HTML执行。不保存会话、不增加数据库/外部调用/模型配置。
未来 provider、超时/取消、重试、会话与存储规则由后续任务定义；当前页面30秒等待上限仅防止浏览器一直等待，不承诺取消服务端工作。

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
