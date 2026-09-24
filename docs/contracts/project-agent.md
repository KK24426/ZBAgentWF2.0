# 项目、需求与 Agent 执行契约

用户于 2026-09-25 批准本轮数据结构、方法签名及配置初始化。当前仅有普通 Bean、抽象方法和配置读取，没有真实 CLI、项目创建、需求规划或任务调度实现。

## 包含关系与数据

Project → List<Requirement> → List<RequirementTask>，均是独立 Java 类，位于 common.project.bean。没有父对象反向引用、持久化字段或外键。

| 类型 | 字段 |
| --- | --- |
| Project | String projectId、Path workingDirectory、List<Requirement> requirements |
| Requirement | String userContent、agentUnderstanding、acceptanceCriteria、userConfirmMsg；List<RequirementTask> tasks |
| RequirementTask | String id、content、acceptanceCriteria；TaskStatus status；AgentExecResult result |
| common.agent.bean.AgentExecResult | String taskId、errorMessage、summary、confirmationMessage；boolean success、confirmationRequired；Long tokenCount |

所有 Bean 提供无参构造与标准 getter/setter，属性原样存取，不自动校验或推断业务状态。requirements/tasks 各自默认 new ArrayList，每个实例独立；setter 原样接收传入列表（包括 null），不复制、不自动建立父子绑定。其它引用默认 null，boolean 默认 false；Task 默认 PENDING。

TaskStatus：PENDING、RUNNING、SUCCEEDED、FAILED、NEEDS_CONFIRMATION。枚举只描述状态，状态转换、调度顺序及失败后是否继续由后续业务定义。
RequirementTask.id 是规划任务标识，AgentExecResult.taskId 是一次执行标识。tokenCount 为 null 表示未知，不等同于零。

## 用户接口

user.project.userif.ProjectUserif implements UserInterface：

- Project newProject(String content)：未来根据描述在配置的根目录下建立项目，将项目自己的目录放入 workingDirectory。
- List<Requirement> createRequirements(Project project, String content)：规划归属于该项目的需求和 Task。
- List<Requirement> execTask(Project project, List<Requirement> requirements)：列表用于选定该项目内的执行范围；等待底层执行结束，返回含 Task 状态和结果的需求列表。

本轮不实现目录创建、列表增删或需求规划，因此不提前决定追加/替换、命名、重试或依赖调度规则。Project 中的包含关系由调用方编排维护。

user.agent.userif.AgentExec 通过构造函数保存 private final AgentBean，protected getAgent() 供实现类读取；工作目录从本次传入的 Project 获取：

- String exec(Project project, String content, String memory, AgentExecCallback callback)：异步受理，返回单次执行标识。memory 没有时可为空。
- String getStderr(String taskId)：读取该次执行的诊断文本；stderr 不决定执行成败，不能原样公开或写日志。
- AgentExecCallback.onCompleted(AgentExecResult result)：每次受理的执行最终回调一次，result.taskId 与返回值一致；不是过程事件。

提交失败同步抛 RejectedExecutionException，不触发回调；受理后的失败通过结果表达。成功为 success=true、confirmationRequired=false；普通失败两者 false；待确认为 success=false、confirmationRequired=true 并提供确认内容。
待确认意味着本次执行已经结束，调用方获得用户答复后重新提交，取得新的执行标识。本轮没有暂停恢复或取消接口，也不把成功和待确认同时设置为 true。

上述异步语义是待实现契约；测试用手动完成的替身说明类型、项目传递和标识关联，不证明真实进程、线程调度、回调可靠性或任务执行能力。

## 项目根目录配置

启动工作目录下 config/project.properties 提供 zb.project.root，示例见 [project.properties.example](../../config/project.properties.example)。真实配置被 Git 忽略，不放进 JAR。
无默认根目录；可通过 ZB_PROJECT_ROOT 或 JVM -Dzb.project.root 覆盖，标准优先级 JVM > 环境变量 > 配置文件。空覆盖值同样无效，不回退到低优先级配置。
目录相对于启动工作目录解析，并规范化为绝对路径。缺失、空白、格式非法、已存在非目录或悬空链接使初始化失败，正常入口退出1；不存在的目录允许，但初始化绝不创建它。

根包 ProjectConfiguration 通过 Spring 初始化只读 ProjectSettings，其 getRootDirectory() 供未来项目实现使用；Project.workingDirectory 是单个项目的实际目录，两者不混用。
Java properties 使用标准转义规则；Windows 推荐正斜线，中文也可用 Unicode 转义。文件只保存根目录设置，不保存账号或凭据。

## 兼容与验收

本次更新接口方法及字段名称，不保留旧签名兼容层；UserImplTest 更名 UserInterfaceTest。调用方需迁移 import 和实现声明。
必填根目录改变无配置启动行为：运行 JAR、IDE 和后续部署前必须提供配置；应用仍不接受命令参数。
验证实体多层包含及独立默认列表、接口替身、Spring 配置与实际 JAR 的加载/覆盖/失败退出；正常 Web 页面、聊天503、原有启动失败路径继续回归。MySQL 环境验收仍独立启用。
