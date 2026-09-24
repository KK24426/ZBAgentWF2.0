# 项目、需求与 Agent 执行契约

用户于 2026-09-25 批准本轮数据结构、方法签名及配置初始化。随后用户批准先接入 Codex 及项目操作规则；当前已有可显式组合的具体实现，模型发现及 Spring 自动装配待确认。

## 包含关系与数据

Project → List<Requirement> → List<RequirementTask>，均是独立 Java 类，位于 common.project.bean。没有父对象反向引用、持久化字段或外键。

| 类型 | 字段 |
| --- | --- |
| Project | String projectId、Path workingDirectory、List<Requirement> requirements |
| Requirement | String userContent、agentUnderstanding、acceptanceCriteria、userConfirmMsg；List<RequirementTask> tasks |
| RequirementTask | String id、content、acceptanceCriteria；TaskStatus status；AgentExecResult result |
| common.agent.bean.AgentExecResult | String taskId、errorMessage、summary、confirmationMessage；boolean success、confirmationRequired；Long tokenCount |

所有 Bean 提供无参构造与标准 getter/setter，属性原样存取，不自动校验或推断业务状态。requirements/tasks 各自默认 new ArrayList，每个实例独立；setter 原样接收传入列表（包括 null），不复制、不自动建立父子绑定。其它引用默认 null，boolean 默认 false；Task 默认 PENDING。

TaskStatus：PENDING、RUNNING、SUCCEEDED、FAILED、NEEDS_CONFIRMATION。普通 Bean 不自动转换状态；项目实现按下述规则更新。
RequirementTask.id 是规划任务标识，AgentExecResult.taskId 是一次执行标识。tokenCount 为 null 表示未知，不等同于零。

## 用户接口

user.project.userif.ProjectUserif implements UserInterface：

- Project newProject(String content)：建立 UUID 项目目录并规划首批需求，将目录放入 workingDirectory。
- List<Requirement> createRequirements(Project project, String content)：成功后追加需求，返回本次新增需求及 Task。
- List<Requirement> execTask(Project project, List<Requirement> requirements)：列表用于选定该项目内的执行范围；等待底层执行结束，返回含 Task 状态和结果的需求列表。

项目具体实现规则见下文；包含关系仍通过 Bean 列表表达。

user.agent.userif.AgentExec implements UserInterface，通过构造函数保存 private final AgentBean，protected getAgent() 供实现类读取；工作目录从本次传入的 Project 获取：

- String exec(Project project, String content, String memory, AgentExecCallback callback)：异步受理，返回单次执行标识。memory 没有时可为空。
- String getStderr(String taskId)：读取该次执行的诊断文本；stderr 不决定执行成败，不能原样公开或写日志。
- AgentExecCallback.onCompleted(AgentExecResult result)：每次受理的执行最终回调一次，result.taskId 与返回值一致；不是过程事件。

提交失败同步抛 RejectedExecutionException，不触发回调；受理后的失败通过结果表达。成功为 success=true、confirmationRequired=false；普通失败两者 false；待确认为 success=false、confirmationRequired=true 并提供确认内容。
待确认意味着本次执行已经结束，调用方获得用户答复后重新提交，取得新的执行标识。本轮没有暂停恢复或取消接口，也不把成功和待确认同时设置为 true。

AgentExecContractTest 的手动替身仅说明契约；具体 Codex 实现通过真实 Java 子进程 fixture 验证，尚未运行真实模型账号验收。

## 项目根目录配置

启动工作目录下 config/project.properties 提供 zb.project.root，示例见 [project.properties.example](../../config/project.properties.example)。真实配置被 Git 忽略，不放进 JAR。
无默认根目录；可通过 ZB_PROJECT_ROOT 或 JVM -Dzb.project.root 覆盖，标准优先级 JVM > 环境变量 > 配置文件。空覆盖值同样无效，不回退到低优先级配置。
目录相对于启动工作目录解析，并规范化为绝对路径。缺失、空白、格式非法、已存在非目录或悬空链接使初始化失败，正常入口退出1；不存在的目录允许，但初始化绝不创建它。

根包 ProjectConfiguration 通过 Spring 初始化只读 ProjectSettings，其 getRootDirectory() 供 ProjectUserifImpl 使用；Project.workingDirectory 是单个项目的实际目录，两者不混用。
Java properties 使用标准转义规则；Windows 推荐正斜线，中文也可用 Unicode 转义。文件只保存根目录设置，不保存账号或凭据。

## 兼容与验收

本次更新接口方法及字段名称，不保留旧签名兼容层；UserImplTest 更名 UserInterfaceTest。调用方需迁移 import 和实现声明。
必填根目录改变无配置启动行为：运行 JAR、IDE 和后续部署前必须提供配置；应用仍不接受命令参数。
验证实体多层包含及独立默认列表、接口替身、Spring 配置与实际 JAR 的加载/覆盖/失败退出；正常 Web 页面、聊天503、原有启动失败路径继续回归。MySQL 环境验收仍独立启用。


## 可调用的具体实现

- agent.codex.CodexAgentExec：异步执行与诊断查询。
- agent.codex.CodexRequirementPlanner：只读规划辅助。
- agent.codex.CodexClient：二者共用的本机进程适配器。
- agent.project.ProjectUserifImpl：项目目录、需求追加及等待异步结果的串行执行。

构造参数显式组合：CodexClient 接收原生可执行文件 Path、CLI 模型 selector String 和正值 Duration；CodexAgentExec 接收 AgentBean 和 client；planner 接收 client；ProjectUserifImpl 接收 ProjectSettings、planner 和 AgentExec。没有把 CLI 版本填入 AgentBean.ver，没有决定三个字段与 selector 的映射。模型列表来源和默认选择待用户答复，AgentBase 仍未实现，以上组件未自动注册到 Spring，网页聊天保持未接入。

## 执行器与进程细节

每次受理返回 UUID。快速执行可在 exec 返回前完成回调，调用者按 result.taskId 关联，不能假设回调发生在返回之后。工作目录在受理时快照。无效输入、无效目录、四个执行槽已满或关闭时同步拒绝且不回调。回调异常只记录脱敏诊断，不重复回调。close 停止受理、中断执行并有限等待；不承诺抵抗 JVM 崩溃或调用方永不返回的回调。

ProcessBuilder 分离 executable、args 和 UTF-8 stdin，固定使用 Codex exec JSONL、临时输出 schema、ephemeral、无颜色和允许普通非 Git 项目。任务 sandbox 为 workspace-write，规划为 read-only，approval 为 never；保留 CLI 原有配置、认证和规则，不使用危险绕过选项。权限或工具错误不能冒充成功，程序不自行扩大权限。
退出码 0、唯一 turn.completed、无 turn.failed/error 且最终 agent_message 为合法结构化数据，才接受协议结果；业务结果仍可失败或待确认。Java 生成执行 ID，usage 的 input_tokens + output_tokens 为 tokenCount，cached_input_tokens 不重复计数；缺失或不可表示时为 null。

三个管道并发处理，整体超时和进程退出后收尾有界，回收主进程及已观察到的后代；ProcessHandle 遍历不等同于操作系统级进程组隔离。stdout 超过 8 MiB 失败，stderr 保留至多 64 KiB 并标明截断、脱敏。schema 临时文件结束后删除；日志隐藏自由文本但保留异常类型、堆栈和关系，单独记录生成标识、固定阶段及退出码。
getStderr 在受理后可查，最终回调前更新，执行中可能为空字符串，未知 ID 返回 null。内容不得直接公开到 HTTP，脱敏无法保证识别任意隐私文本。诊断只保留在内存直到 close，关闭后清空且不再接受迟到写入。总量随完成次数增长，必须管理实例生命周期；长驻装配前确定总量上限或淘汰策略。

## 项目具体行为

newProject 验证非空内容，在 root/UUID 建目录并规划首批需求。只有业务调用才创建尚不存在的根目录，初始化不创建。失败抛 IllegalStateException，仅尝试删除本次创建且仍为空的项目目录，绝不递归删除；保留根目录和外部写入的文件。
createRequirements 校验项目目录存在且真实路径位于根目录之下，完整规划成功后把列表替换为“旧需求 + 新需求”，原需求对象保留，返回本次新增列表；外部持有的旧列表引用不自动同步。这兼容 Bean setter 接受的不可变列表，失败不追加部分数据。
规划只读分析原始内容和目录，返回理解、验收和按顺序执行的 Task；每条 userContent 保存本次完整输入。Java 生成 Task UUID，默认 PENDING、result=null。信息不足时 userConfirmMsg 为待确认事项且 tasks 为空，不自动发明业务规则。规划失败的受限脱敏 stderr 保留在 cause 的 suppressed 异常中供本地诊断，顶层错误仍固定；不得原样外传或用其它日志组件直接打印这段自由文本。

execTask 开始前按对象身份检查需求归属，拒绝重复需求、共享 Task、无效 Task 标识/状态/内容及目录越界。同一实现实例对同一真实项目目录串行操作，调用方不要在执行期间直接并发修改 Bean。
按传入需求及 Task 列表顺序，只执行 PENDING。在本次范围内遇到既有 FAILED/NEEDS_CONFIRMATION 也立即返回，不能再次调用就自动绕过；SUCCEEDED 可跳过。提交时先快照上下文再置 RUNNING、清旧结果，受理失败恢复 PENDING 和旧结果并抛 RejectedExecutionException；受理后等待回调，写回结果并映射 SUCCEEDED、FAILED 或 NEEDS_CONFIRMATION。失败或待确认立即返回，后续任务不变。
调用方补充确认相关内容、将目标 Task 重置为 PENDING 后重试，保留规划 id，替换单次结果。上下文包含当前需求原始内容、理解、验收、确认相关内容，以及按规划任务和旧执行 ID 关联的先前摘要、确认问题、失败原因，在清旧结果前生成；简短答复也能关联原问题。不跨项目共享记忆。等待线程中断后仍等待已受理执行完成，再保留中断标志返回，不启动下一项，没有新增取消协议。

## 实现验证边界

真实 Java fixture 子进程验证协议、中文 stdin/cwd、并发管道、非零退出、错误结果、确认、超时、回调一次、关闭及后代管道收尾；项目测试验证创建、追加、串行、停止、人工重试和数据隔离。fixture 不进入正式 JAR，完整 verify 继续覆盖 Web/JAR。未运行真实 Codex 登录、账号模型可用性或端到端模型验收，没有新增持久化、Git 操作、HTTP 路由或部署。
