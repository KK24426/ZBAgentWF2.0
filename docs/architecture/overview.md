# 架构总览

单 Maven 工程，按包组织，不采用 Maven 子模块隔离。
根包为 com.kk24426.zbagentwf，根启动类初始化日志与 Spring Web 容器。
user 放用户接口/编排，agent 放接口实现、HTTP Controller 和持久化，common 放共享 Bean/工具及跨包使用的技术异常。
启动 -> 独立日志 -> Spring配置加载与扫描/注入（校验必填项目根目录） -> 内嵌Tomcat持续处理HTTP -> 停止信号 -> 优雅关闭容器/连接池和日志。

Spring Boot 4.1.1；MyBatis Starter 4.1.0；Java26；Maven Wrapper3.9.16。
未激活 mysql 时没有 DataSource；激活后验证配置和连接，失败退出1。
当前首页包含单次聊天入口：agent.chat.controller.ChatController -> user.chat.service.ChatService -> user.chat.api.AgentChat -> agent.chat.AgentChatImpl。正式实现明确尚未接入；成功路径由测试替身验证，无业务表或真实Agent执行。
ProjectConfiguration 在根包初始化只读 ProjectSettings；zb.project.root 必填，初始化只读配置及目录元数据，不创建目录。
common.project.model 保存 Project→Requirement→RequirementTask；user.project.api 和 user.agent.api 保存用户接口。agent.codex 提供 CodexClient、CodexAgentExecutor 和只读规划器，agent.project.ProjectDomainImpl 实现项目操作和串行执行。AgentConfiguration装配配置驱动的AgentCatalog、AgentExecutorFactoryImpl及项目服务；Project绑定规划/开发/审核三个执行器，agent.runtime共享额度和诊断缓存；已通过当前本机Codex的有限项目功能验收，范围见[验收记录](../operations/codex-live-validation.md)。AgentChatImpl 仅实现 AgentChat，不继承 AgentRegistry，聊天入口仍未接入。
WebRequestFilter限制固定资源和/api/chat POST、记录安全请求摘要；后续路由仍须按批准契约同步调整。
MsgConfiguration启动时显式加载三语msg及可选外部覆盖；common.msg.MsgCatalog提供只读消息查询，agent.web.MsgLocaleResolver共享请求级语言，HomePageController在原首页路径渲染非公开模板。页面脚本从inert template读取三语消息并即时切换，ChatController/Filter查询同一消息目录。详见[本地化契约](../contracts/msg.md)与[ADR0011](../decisions/0011-message-localization.md)。
远程测试服务与专用MySQL库部署在同一已批准服务器，Web只绑定回环地址，通过SSH隧道访问。
旧项目不自动搬运；后续真实Agent调用、会话、数据存储仍由用户定义。
决策依据见 [ADR0006](../decisions/0006-single-project-spring-mysql.md) 和 [ADR0007](../decisions/0007-web-test-service.md)。
项目契约与配置决定见 [ADR0009](../decisions/0009-project-contracts.md)。
模型注册、工厂与项目角色决定见 [ADR0010](../decisions/0010-agent-registry-project-roles.md)。

项目HTTP链路为agent.project.controller.ProjectController -> user.project.service.ProjectService -> user.project.api.ProjectDomain -> agent.project.ProjectDomainImpl，通过common.memory.MemoryStore登记本进程对象。规则由PromptCatalog显式加载，RoleAgentResolver绑定角色和模型；同模型的不同实体使用独立执行器，仍共享运行资源。决定见[ADR0012](../decisions/0012-project-memory-role-prompts.md)，Controller归包见[ADR0013](../decisions/0013-java-naming-and-packages.md)。

项目工作台位于/projects（兼容/projects.html），由agent.web.ProjectPageController渲染；与HomePageController共享包内LocalizedPageRenderer和messages.js三语选择。projects.js消费已有项目HTTP快照，不新增业务接口、浏览器持久化或自动审核链路。
