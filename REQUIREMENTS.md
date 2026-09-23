# 项目需求与验收

## 当前范围
单 Maven JAR 工程；Java 26、Spring Boot 4.1.1、MyBatis Starter 4.1.0。
启动入口初始化日志和 Spring Web，扫描 user/agent/common，持续运行。
当前有简单首页及单次聊天调用骨架；真实 Agent、业务表和持久化尚未接入；原 CLI 已移除。

## 用户骨架与最小补充

用户已提供 AgentBean、UserImpl、AgentBase、UserService 四个类型，目前不代表业务能力。
AgentBean 保留 brand/name/ver 三个私有 String 属性，提供标准 getter/setter 和无参构造；默认 null，允许 null、空字符串及中文，原样存取、不校验、不设置默认值。
UserImpl 经用户确认作为公共空父接口，用户接口通过 extends 继承；不新增业务方法。
AgentBase 和 UserService 保留用户原始占位代码，不补行为、注解或继承关系。
验收包括属性独立读写、边界值、父接口继承实现关系，以及既有 Spring 和真实 Web JAR 回归；测试样例不进入正式 JAR。
此最小补充仅用于验证协作流程，不代表复杂业务接口、模型调用或数据库持久化已实现或验收。

## Web
无参数启动服务，默认 127.0.0.1:8080；远程仅通过 SSH 隧道访问，不开放公网。
固定资源 /、/index.html、/app.css、/favicon.svg、/chat.js 接受 GET/HEAD；/api/chat 仅接受 POST。
未批准的路径/方法继续拒绝；聊天错误响应为固定文字，不暴露异常或原始输入。
stdout 不输出业务结果，stderr 为错误及运行诊断，异常保留脱敏后的完整调用链。
启动失败退出1、命令参数错误退出2；正常运行不主动退出，正常停止有20秒优雅关闭窗口。
正式产物 target/zbagentwf-web-0.1.0-SNAPSHOT.jar；不承诺 Maven 类库兼容。

## 单次聊天骨架
user.chat.controller.ChatController 接收请求；user.chat.service.ChatService 继承 UserService，调用 AgentChat（继承 UserImpl）；agent.chat.AgentChatImpl 继承 AgentBase 实现接口。原父类/父接口及 AgentBean 保持不变。
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
真实 Agent provider、调用配置及协议；后续业务表结构、事务边界及会话功能；
生产环境与权限模型（当前仅批准远程测试环境、专用测试库和受限账号）；
日志历史保留策略若需要自动清理，由后续任务定义。
