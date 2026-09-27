# 本地开发

需要JDK26；Windows用mvnw.cmd，macOS/Linux用./mvnw（首次下载需要unzip和sha256sum或shasum）。
Wrapper固定Maven3.9.16。依赖缓存不进入仓库。本地使用Windows，远程测试使用Linux容器。

## 构建与启动

```powershell
.\mvnw.cmd clean verify
$env:ZB_PROJECT_ROOT = 'PATH_TO_PROJECT_ROOT' # 先替换为实际根目录
java -jar .\target\zbagentwf-web-0.1.0-SNAPSHOT.jar
```
verify运行单元测试与真实Boot JAR进程测试；MySQL专用库测试默认跳过。
项目根目录必须显式配置，没有默认目录。配置缺失、空白、格式非法或已存在非目录时启动失败退出1。
可以使用上面的环境变量，或复制 [config/project.properties.example](../../config/project.properties.example) 为启动工作目录下的 config/project.properties，填写 zb.project.root；真实文件不提交。
JVM -Dzb.project.root 优先于 ZB_PROJECT_ROOT，再优先于文件值；空覆盖值不会回退到低优先级配置。应用仍不接受 --zb.project.root 等命令参数。
相对路径按启动工作目录解析并规范化为绝对路径；允许目录不存在，但初始化不创建它，也不会创建业务项目。全局根目录与 Project.workingDirectory 中的单个项目目录区分。
properties 文件使用标准 Java 转义；Windows 推荐正斜线，中文路径可写 Unicode 转义，避免反斜线被解释为转义字符。
浏览器访问 http://127.0.0.1:8080/，首页可输入请求并发送；当前正式Agent为未接入占位，显示503提示。成功链路仅在测试中由替身验证，替身不随JAR发布。使用Ctrl+C正常停止。默认只监听回环地址。
正式JAR为target/zbagentwf-web-0.1.0-SNAPSHOT.jar，.jar.original不是正式分发。
不会生成或分发旧apps/runtime-host产物。旧apps和modules目录不再使用，
当前源码统一位于根src目录，旧目录中的构建缓存和IDE配置不属于有效工程内容。

隔离验证时同时设置MAVEN_USER_HOME（Wrapper缓存）及-Dmaven.repo.local（依赖缓存），
二者使用任务专属临时目录；结束后恢复环境。不得清理整个用户.m2缓存。

## Eclipse
从工作区移除旧的父工程及四个子工程引用（不要勾选从磁盘删除内容），
再通过 File > Import > Maven > Existing Maven Projects 选择仓库根目录。
只导入根pom，选择JDK26，执行Maven Update Project。
在根包ZbAgentWfApplication上Run As > Java Application，不传入Program arguments；在运行环境配置 ZB_PROJECT_ROOT，或使用运行工作目录下的 config/project.properties；启动后持续运行。
CLI及help/version已移除。Eclipse强制Terminate可能不触发优雅关闭；验证正常停机请用终端Ctrl+C或远程容器stop。
XML的Cannot find declaration错误应检查XML插件外部Schema下载设置；
POM沿用Maven官方声明，不通过关闭所有XML校验解决。
清理旧目录前应先移除Eclipse中的旧子工程引用，否则运行中的IDE可能重建旧.project文件和目录。
根工程名称使用zbagentwf。Eclipse工作区可能仍缓存旧工程引用：先选中工程按F5刷新；
如仍显示旧工程，按上述步骤移除引用后重新导入，再清理已核实仅含缓存和IDE元数据的旧目录。
只保留根工程；本地IDE元数据仍不提交到Git，不直接修改Eclipse工作区.metadata。

## 页面语言与消息覆盖

复制[语言配置示例](../../config/msg.properties.example)为启动工作目录下的`config/msg.properties`，设置`zb.msg.locale=auto`（默认）、`zh-CN`、`en`或`ja`。也可设置`ZB_MSG_LOCALE`或JVM `-Dzb.msg.locale`，JVM优先。`auto`按浏览器语言选择，不使用服务器操作系统语言；无支持语言时默认中文。配置修改后重启。

页面右上角的语言选择优先于服务配置，并通过当前浏览器的`zb.locale` Cookie保存一年。选择“自动”清除手动偏好。切换不会清空输入、改变模型回复或重发正在执行的请求。禁用Cookie的浏览器不能保证刷新后记住选择。

需要修改文案时，将[消息示例目录](../../config/msg/)中的相应`.properties.example`复制为同目录下的`.properties`文件，只写需要覆盖的已知key。文件必须为UTF-8，三语可分别覆盖，缺省条目使用内置同语言文案。比如`chat.sending=正在发送…`，或`chat.requestId=请求编号：{0}`。不要删掉原文需要的参数，不写HTML模板或秘密信息；未知key、空白值、重复key、非法编码/格式、参数不一致都会启动失败。修改后重启。

真实配置文件已被Git忽略，只有内置消息和示例入仓。详细key、优先级、参数及HTTP边界见[消息契约](../contracts/msg.md)。翻译测试使用临时目录，不修改开发者的真实外部配置。

本地化Java测试与真实JAR回归纳入普通`mvnw.cmd verify`。首次从旧静态首页版本构建应执行`mvnw.cmd clean verify`，清除target中的旧首页资源。
浏览器验收可在临时工作目录启动回环测试JAR，再用Python 3执行`src/test/browser/localization_fixture.py --upstream-port <JAR端口>`。替身只绑定127.0.0.1并打印随机端口，代理真实页面；输入`fixture:delayed`在8秒后返回固定成功文本，`fixture:timeout`在35秒后返回以验证页面30秒超时，`fixture:invalid`返回格式错误，其余输入返回模拟503。它用于验证切换过程中按钮/输入/结果和安全显示，不代表真实模型验收，不属于应用运行依赖；验收结束停止替身与测试JAR。

## 日志
文件：当前工作目录/logs/<runId>/application.log；UTF-8，默认项目DEBUG/框架INFO。
目录优先级：JVM -Dzb.log-dir > ZB_LOG_DIR > logs。
单文件20MB或跨日滚动压缩，不自动删除；多进程目录独立。
可以在确认相关进程已结束后手工删除对应runId目录以释放空间，不删除仍在写入的目录。
JVM -Dzb.log-max-size可调整滚动大小；日志级别用标准logging.level属性。
stderr日志与固定错误提示统一跟随JVM System.err编码，文件日志始终UTF-8；
程序捕获时可显式使用-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8统一。
异常链完整记录且已知敏感消息脱敏，不保证识别任意秘密；不要直接打印输入Bean、密码或SQL参数。
请求按requestId关联，只记录固定路由类别、白名单方法、状态和耗时，不输出原始URL、查询或正文。
Tomcat解析前错误尚未进入过滤器，不带应用requestId；协议诊断使用固定摘要，保留来源、级别和异常调用链，隐藏原始请求内容。
启动日志故障会退出；运行期检测到日志故障后后续请求返回503，修复日志目标后重启服务。

## MySQL运行配置
默认不连接数据库。只有开启mysql才加载连接池、Mapper和事务。
通过环境变量提供：
- SPRING_PROFILES_ACTIVE=mysql
- SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/YOUR_DATABASE
- SPRING_DATASOURCE_USERNAME、SPRING_DATASOURCE_PASSWORD：自行在本地配置，不能提交。

也可使用外部application-local.properties，激活mysql,local；仓库忽略该文件。
不接受命令参数，不能用java -jar ... --spring.profiles.active=mysql代替环境变量或JVM -D。
启用mysql时启动检查会建立真实连接，失败不进入就绪状态。
不创建数据库/表，不执行初始化SQL或migration，不降级H2。
Hikari最大5、空闲0、取连接超时5秒，驱动连接超时5秒、读取超时30秒。
配置不足或连接失败退出1，检查对应runId日志。

## MySQL专用库验证
使用已批准的专用zbagentwf_test库和仅有该库建表、删表、CRUD权限的账号。
远程测试环境通过SSH隧道连接，见[远程测试环境](./remote-test.md)；不使用root或操作现有业务库。
未经明确授权不查找系统保存的密码。
测试只接受以下专用环境变量，不回退到SPRING_DATASOURCE：
- ZB_TEST_DB_URL=jdbc:mysql://127.0.0.1:3306/zbagentwf_test
- ZB_TEST_DB_USERNAME、ZB_TEST_DB_PASSWORD：在本地设置。

URL仅允许localhost或127.0.0.1及可选端口，不能带URL参数、其它主机或其它库。
```powershell
.\mvnw.cmd -Pmysql-it verify
```
启用该profile后缺配置必须失败。测试先校验catalog，再创建随机zb_it_表，验证CRUD、中文和事务回滚，
结束只DROP本轮CREATE成功的测试表；不会DROP DATABASE。
不要将普通verify成功视为MySQL测试通过；实际远程验收情况以对应任务交接为准。


## 模型注册与项目调用

复制 [agents.properties.example](../../config/agents.properties.example) 为启动工作目录下的 config/agents.properties，填写模型三元组、type=codex、原生可执行文件路径、实际CLI模型参数、单次timeout和enabled。真实配置被Git忽略；不保存登录材料。启动时加载，修改后重启。条目键如[0]只用于配置分组，模型身份仍是brand/name/ver；同一模型三元组重复会失败，单字段可由JVM属性或环境变量覆盖。
无注册配置仍可启动Web，查询可用Agent时明确报错。初始化只检查已配置路径是否为可执行普通文件，Windows要求原生.exe；不能把此检查当作账号鉴权或模型可调用验收。refreshAgentList仅重查文件状态。CLI安装、登录及原有规则由本机使用者管理，应用不读取或复制凭据。

由Spring注入user接口后调用，业务代码无需构造Codex具体类：

```java
// agentBase、agentExecFactory、projects 分别注入 AgentRegistry、AgentExecutorFactory、ProjectDomain。
AgentBean planning = agentBase.getActiveAgent("YOUR_PROVIDER", "YOUR_MODEL", "YOUR_PLANNING_VERSION");
AgentBean development = agentBase.getActiveAgent("YOUR_PROVIDER", "YOUR_MODEL", "YOUR_DEVELOPMENT_VERSION");
AgentBean review = agentBase.getActiveAgent("YOUR_PROVIDER", "YOUR_MODEL", "YOUR_REVIEW_VERSION");
Project project = projects.newProject("用户项目描述", planning, development, review);
// 检查规划后再由业务调用方启动任务。
projects.execTask(project);
AgentExecutor executor = agentExecFactory.getExecutor(development);
// executor.exec(project, content, memory, callback) 也可直接使用。
```

项目持有planningAgent/developmentAgent/reviewAgent；工厂按Agent实体隔离实例并管理关闭，调用方不关闭共享执行器。规划绑定必须来自当前工厂；审核角色只绑定，不自动运行审核。Project执行器引用用于内存运行，不承诺序列化或持久化。newProject(content)读取显式三角色默认配置，完整指定三个Agent则使用用户选择。
创建项目先验证三个角色，随后在root/UUID创建目录并以read-only规划；execTask才以developmentAgent执行workspace-write任务。需要确认时结束本次执行，调用者补充内容并重置PENDING后重新提交。CLI协议继续基于已适配的Codex exec能力；升级后另行做实际账号验收。

共享工厂同时最多受理4个规划/执行，无队列，执行超限同步拒绝且不回调。单次任务超时由各条目的timeout指定，示例30m只是可调整示例。stderr诊断单条64KiB，完成记录全工厂最多256条/30分钟；未知、过期和其它执行器所有ID返回null，不影响Task结果。stdout上限8MiB。直接构造低层Codex类的调用方负责自己的资源作用域和close，应用业务应使用共享工厂。
正常关闭事件立即停止受理并中断工作；共享Agent资源从该时刻起最多等待20秒，不把20秒当作单任务超时。销毁阶段等待票据归还而非长期调用线程退出，预算不重新计时。Spring Web另有每phase关闭预算，不能推断整个应用任何情况下20秒内退出。
测试调用真实Java fixture子进程，不消耗模型额度；真实Codex模型验收按用户决定延后，网页聊天保持503。完整verify包含Web/JAR回归，MySQL仍单独启用。

## 角色默认值、规则与内存项目

在config/agents.properties中按示例补充zb.agent-roles.planning/development/review的brand/name/ver，指向已注册模型；不同角色可选同一模型，也可完全不同。HTTP或Java调用显式提供完整三个模型时覆盖默认；无默认值只影响需要默认的项目创建，不影响Web启动。
将config/prompts/五个.txt.example复制为同名.txt，替换YOUR_占位为你自己的UTF-8规则；示例不能直接用于调用。default/security为通用必要规则，planning/development/review供对应角色绑定使用。文件在启动时读取，修改后重启；不要在规则中保存凭据。真实文件被忽略。未准备配置时项目创建返回固定503，不调用模型；真实Agent仍按既有安排另行验收。
项目HTTP操作、请求格式及状态码见[项目契约](../contracts/project-agent.md#项目-http-入口)。例如向POST /api/projects发送 {"content":"项目需求"}，成功后保留返回的projectId；之后GET /api/projects/{projectId}取回快照，POST相应子路径追加提示词/需求或执行任务。创建和规划会实际调用配置的Agent；不要把接口可达当作真实模型验收。
MemoryStore保存任意类别对象；项目仅在本次服务运行内有效。没有自动淘汰或恢复，应用重启会丢失登记。配置修改不会热替换已绑定模型；停机后项目目录可能仍在，但仅目录和ID不足以恢复原需求/执行器。
