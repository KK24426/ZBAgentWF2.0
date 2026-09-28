# 项目、需求与 Agent 执行契约

2026-09-28用户明确批准补齐项目执行器接口、不可变Prompt、安全模型预审、四表持久化与root/固定用户/projectId目录。本轮以用户最新接口及已确认方案为准，替代此前按模型实体建执行器及项目仅存内存的约定。2026-09-27的[真实模型记录](../operations/codex-live-validation.md)仅代表当时版本，不代表新审核/持久化链路的真实模型验收。

## 包含关系与身份

Project → List<Requirement> → List<RequirementTask>。AgentBean、Project、Requirement、RequirementTask继承DataBean：Long id为数据库生成主键，creationData/lastupdateData为时间，delFlg为逻辑删除标志，version用于乐观锁。保留用户原字段名称。

| 类型 | 业务数据 |
| --- | --- |
| Project | UUID字符串projectId；可空projectName；三角色AgentBean；projectPrompt；requirements |
| Requirement | userContent、agentUnderstanding、acceptanceCriteria、userConfirmMsg、tasks |
| RequirementTask | content、acceptanceCriteria、status、result；id来自DataBean |
| AgentBean | brand/name/ver、rolePrompt、skill、think；think保存推理程度，本轮不自动映射CLI参数 |
| AgentExecutionResult | String taskId/errorMessage/summary/confirmationMessage；boolean success/confirmationRequired；Long tokenCount |

Project使用用户提供的五参数构造器，不再保存workingDirectory或运行时执行器。其他可变Bean保留标准存取，列表setter不自动复制；并发修改必须遵守项目锁。Task默认PENDING，其他状态为RUNNING、SUCCEEDED、FAILED、NEEDS_CONFIRMATION。数据库Task.id、业务projectId和一次执行的result.taskId互不混用。

Prompt为final不可变类型，使用Prompt(String)或Prompt(String, byte[])构造。构造及getFile均复制附件数组，构造不调用外部模型；没有setter。file仅为后续多模态保留，当前非空附件在调用和持久化前明确拒绝，不静默丢弃；不实现上传或文件解码。

## 用户接口

ProjectDomain使用构造注入DataBeanDao<Project>，ProjectDomainImpl提供完整持久化与目录实现。保留用户的createRequiremens拼写，不保留失效的旧方法壳。

| 入口 | 行为 |
| --- | --- |
| newProject(content, projectName, planning, development, review) | 验证三角色并创建UUID及目录，两阶段规划全部成功后保存完整项目 |
| newProject(content, projectName) | 使用三个显式角色默认值；缺配置不推断替代模型 |
| createRequiremensOnNew(Project, content) | protected；对已有UUID/角色上下文的新项目返回未发布的完整规划列表 |
| createRequiremens(Project, content) | 规划全部成功后追加并保存，返回本次新增需求 |
| createRequirementsTask(Project, Requirement) | 只处理本项目内尚无任务、无需确认的需求；保存成功后返回任务 |
| getProject(projectId) | 从数据库完整重载，原子取得并保留本进程唯一项目对象身份 |
| addProjectPrompt(projectId, content) | 追加非空文本并保存；之后的完整调用仍须审核 |
| execTask(Project) | 等待批次回调，返回含任务状态和结果的需求列表；不持项目锁等待worker |

AgentExecutorFactory.getExecutor(Project)返回ProjectAgentExecutor。每个项目对象一个协调器，规划/开发角色使用不同配置的客户端。相同项目复用；项目UUID、模型三元组、角色规则、skill或think变更后再次获取拒绝悄悄重绑。实体数据库元数据变化不影响绑定。工厂拥有ExecutionResources及协调器生命周期，业务调用者不关闭共享资源。

ProjectAgentExecutor保存final Project并提供受保护的上下文读取、exec()和securityCheck()；getAllPrompt()返回当前线程完整输入，非执行线程只返回开发规则快照。

- execAllTasks(callback)：受理时快照任务ID范围和规则，返回批次UUID；按需求/任务顺序处理，终态回调一次，每个真正执行的Task获得独立执行UUID。
- execTasks(Long taskId, callback)：只受理当前项目内PENDING的数据库任务，返回该次执行UUID，其他任务不执行。
- getStderr(executionId)：读取此协调器受理的批次/单任务受限诊断，不是业务结果，不公开到HTTP。

无效输入、目录、容量不足、关闭中等受理前失败同步抛RejectedExecutionException且无回调。受理后无论成功、模型失败、审核拒绝或存储异常，最终回调一次；快速完成可早于方法返回，按result.taskId关联。回调前归还票据并释放项目锁，回调异常不重复回调。

## 安全审核与调用顺序

每次需求规划、需求拆任务和任务执行先构造最终不可变Prompt，再由同一已配置模型发起独立read-only审核。审核使用security规则和固定审核职责，将待执行提示词、真实目录、schema和运行模式作为JSON数据，不让payload决定审核角色。仅严格合法的approved=true且reason非空才通过；拒绝、错误、超时、无效JSON或字段不符都阻止业务调用。审核自身不递归审核。

通过后只签发不可外部构造、绑定当前客户端/Prompt/真实目录/schema/模式的单次许可。执行前再次检查中断、关闭和真实目录，许可不能转移、修改或复用。之后不再拼接未审核内容。规划及审核用read-only，开发业务用workspace-write，固定approval=never。Prompt校验和模型预审不能证明所有请求绝对安全；操作系统沙箱、目录约束与原CLI规则继续生效。

审核沿用当前业务角色的模型，不自动执行reviewAgent的业务代码审查。审核与业务调用各自使用配置的单次timeout；同一票据顺序执行，不多占共享额度。已知时tokenCount计入审核及业务用量，任何部分未知则为null。

ProcessBuilder分离可执行文件、args和UTF-8 stdin；不提供任意shell字符串。使用Codex exec JSONL、ephemeral及输出schema，保留本机登录和配置，不复制凭据。退出码0、唯一turn.completed、无error/turn.failed且最终消息合法才接受；业务字段仍可表达失败或待确认。

## 两阶段规划和执行状态

需求规划仅生成理解/验收/确认问题；对信息充分的每条需求另发一次只读任务拆分。任何阶段失败不追加部分列表；已有任务不覆盖。模型不产生数据库ID，新任务PENDING、result=null。各阶段均审核自己的完整Prompt。规划失败保留受限脱敏诊断供本地排查，HTTP仅固定错误。

执行在同一Project对象锁内串行，调用方不直接并发改Bean。SUCCEEDED跳过；遇到新产生或既有FAILED、NEEDS_CONFIRMATION、RUNNING立即停止后续任务。先生成包含需求、验收、确认和历史结果的最终Prompt，再保存RUNNING并清旧结果；保存失败不调用审核/业务模型。审核或模型结束后保存Task终态与结果，再考虑下一项。普通失败两布尔值false，待确认success=false/confirmationRequired=true并带问题，成功success=true/confirmationRequired=false。

批次只最终回调一次；汇总token用量、受限stderr，失败/确认停止。调用方人工补充内容并将目标重置PENDING后才可能重试，本轮无HTTP重置、自动重试、暂停恢复或取消入口。数据库重载得到RUNNING时不假设尚未执行，不自动重跑。调用线程中断仍等待已受理工作收尾并恢复中断标志。

## 数据库与事务

正式DDL为src/main/resources/db/project-schema.sql，生产Mapper为ProjectMapper及同名XML；显式mysql profile启用MyBatis和连接池。应用不自动建库、建表或迁移。

| 表 | 范围 |
| --- | --- |
| zb_agent | 每项目三个独立角色快照，包含think/角色规则/skill名，不保存CLI路径和凭据 |
| zb_project | 业务UUID唯一、可空名称、角色外键、项目规则 |
| zb_requirement | 项目外键、顺序、原文、理解、验收、确认信息 |
| zb_task | 需求外键、顺序、内容、验收、状态与JSON执行结果 |

所有表包含DataBean公共元数据，子表父键+顺序唯一。插入/更新完整聚合在一个REQUIRES_NEW短事务中完成；多项目保存同样整体提交。更新检查每行version、既有角色和父子归属，不从列表缺失推断物理删除。保存前拒绝跨批次共享可变实体及独立删除项目角色。已保存子项保留原排序槽，新项在含删除行的最大槽后追加；已删除子项可省略，仍保留删除标记的旧快照不重复更新，不支持清标记复活。提交成功前只写技术行，不把生成ID/新version提前发布给用户对象。

SQL或提交异常隔离当前对象，不能用内存状态自动重试；先成功从数据库完整重载才解除隔离。完整读取使用REPEATABLE_READ只读短事务，避免混合多次提交的子表。模型调用、目录和文件修改不在数据库事务内；落盘文件不能随数据库回滚，执行后保存失败需人工核对。

canonical缓存只维护本进程对象身份，不是存储后备；并发首次加载原子登记。未启用mysql时Web能启动，项目操作503。已启用但表缺失/查询或提交故障使用安全500，不返回旧缓存假装成功。MemoryStore保留通用临时对象工具能力，但不再存储项目。服务重启后可按ID读取已有数据库项目；尚未迁移的旧内存项目不自动导入。

## 目录和配置

zb.project.root仍必填，由启动工作目录config/project.properties提供，可用ZB_PROJECT_ROOT或JVM -Dzb.project.root覆盖；优先级JVM > 环境变量 > 文件，空值拒绝。初始化只验证，不建目录。相对路径按启动目录规范化为绝对路径。

具体目录为root/local/projectId，local暂代用户ID，没有登录或多用户权限含义。projectId仅接受规范UUID，不接收客户端路径。创建及调用核对真实路径，拒绝local或项目目录被链接重定向。三角色、必需规则和数据库可用后才创建目录。持久化前规划失败只尝试删除本次新建的空项目目录；进入持久化后失败保留目录，防止提交结果不确定时误删。保留根目录和任何非空项目文件，不递归删除。

模型注册zb.agents[条目键]字段brand/name/ver/type/executable/model/timeout/enabled不变，精确三元组、重复拒绝、当前仅codex。初始化仅检查本机原生可执行文件，Windows要求.exe，不代表登录/模型可用验收。刷新只重查已加载配置文件状态；配置变更需重启。角色默认值zb.agent-roles.planning/development/review的brand/name/ver独立显式配置，完整用户选择优先，不按品牌推断。

## 规则绑定

PromptCatalog启动时读取config/prompts/{default,security,planning,development,review}.txt。UTF-8接受BOM，无热加载；不存在、空白或YOUR_占位在使用时拒绝，不可读/非法编码使启动失败。真实文件忽略，示例入仓。工厂构造后显式initializePrompts；安全、通用、角色、用户、项目规则分段组装，构造/getter不读取文件或调用模型。项目规则追加只影响后续受理，受理批次保留规则快照。planning规则应允许需求和任务两种阶段schema，不能强制每次同时输出二者。Skill仅存名称，不自动安装或执行。

## 资源限制

工厂共享4个规划/执行票据，无队列；一次审核和其业务调用占同一票据。stdout上限8MiB，单次及批次诊断至多64KiB、脱敏并标明截断；完成诊断最多256条/30分钟，运行中不淘汰，未知/过期/其他协调器ID返回null。临时schema在finally清理；回收主进程和已观察后代，不保证OS级完整进程树隔离。

关闭事件停止受理并中断票据，工厂close沿用单一20秒预算等待归还，不等待任意回调永远返回；关闭清空诊断并拒绝迟到写入。Spring Web的phase预算独立，不承诺整应用任何情况20秒退出。

## 项目 HTTP 入口

ProjectController → ProjectService → ProjectDomain。页面只发送ID及文本/模型选择，不接受目录、运行时对象或安全许可。

| 方法和路径 | 输入 | 成功 |
| --- | --- | --- |
| POST /api/projects | content、可选projectName；可选完整planningAgent/developmentAgent/reviewAgent各brand/name/ver | 201快照 |
| GET/HEAD /api/projects/{projectId} | 无 | 200快照；HEAD空体 |
| POST /api/projects/{projectId}/prompts | content | 204 |
| POST /api/projects/{projectId}/requirements | content | 200快照 |
| POST /api/projects/{projectId}/tasks/execute | {} | 200终态快照 |

响应增加可空projectName；保留projectId/requirements，需求字段和Task/result字段不变。Task.id仍是JSON字符串，内容改为Long主键十进制；消费者不得继续假设UUID。未知项目404，输入错误400，配置/未启用数据库/容量拒绝503，内部异常500。固定三语消息、CSP、无缓存、请求编号及回环监听保留。响应不暴露规则、数据库版本、模型配置、路径或stderr。Controller不持项目锁等待worker；所有项目响应在同一canonical对象锁内再次重载数据库并复制快照，防止初次查询后并发保存失败泄露未提交终态，重载失败不返回旧快照。PROJECT日志隐藏自由文本并保留异常关系。原/api/chat仍是独立未接入骨架。

## 项目页面与验证边界

/projects及/projects.html支持可选名称、创建规划、按ID读取/刷新、追加规则/需求、执行待办五项既有操作。业务内容纯文本；页面不保存业务数据，数据库项目可在重启后按ID打开。无项目列表、删除、单任务HTTP、重置、取消或自动业务审核按钮。

单页请求单飞且不自动超时重发；已知项目的写入失败保留旧快照并锁定写入直到成功查询，不能将旧数据当作未执行证明。200按Task状态显示，不把HTTP成功当模型成功。

自动验证覆盖真实Java fixture进程、安全拒绝/非法响应/超时阻断、单次许可、资源回收、两阶段规划、任务选择/批次、HTTP和真实JAR；fixture不等于真实模型安全能力验收。mysql-it额外以随机前缀四表运行正式DDL、Mapper/XML及ProjectDao，覆盖生成ID、重载、版本冲突、父子归属、整体回滚及逻辑删除；只清理本测试成功创建的表。实际运行结果以本轮交接为准。
