# 本地开发

需要JDK26；Windows用mvnw.cmd，macOS/Linux用./mvnw（首次下载需要unzip和sha256sum或shasum）。
Wrapper固定Maven3.9.16。依赖缓存不进入仓库。本地使用Windows，远程测试使用Linux容器。

## 构建与启动

```powershell
.\mvnw.cmd clean verify
java -jar .\target\zbagentwf-web-0.1.0-SNAPSHOT.jar
```
verify运行单元测试与真实Boot JAR进程测试；MySQL专用库测试默认跳过。
浏览器访问 http://127.0.0.1:8080/，使用Ctrl+C正常停止。默认只监听回环地址。
正式JAR为target/zbagentwf-web-0.1.0-SNAPSHOT.jar，.jar.original不是正式分发。
不会生成或分发旧apps/runtime-host产物。旧apps和modules目录不再使用，
当前源码统一位于根src目录，旧目录中的构建缓存和IDE配置不属于有效工程内容。

隔离验证时同时设置MAVEN_USER_HOME（Wrapper缓存）及-Dmaven.repo.local（依赖缓存），
二者使用任务专属临时目录；结束后恢复环境。不得清理整个用户.m2缓存。

## Eclipse
从工作区移除旧的父工程及四个子工程引用（不要勾选从磁盘删除内容），
再通过 File > Import > Maven > Existing Maven Projects 选择仓库根目录。
只导入根pom，选择JDK26，执行Maven Update Project。
在根包ZbAgentWfApplication上Run As > Java Application，不传入Program arguments；启动后持续运行。
CLI及help/version已移除。Eclipse强制Terminate可能不触发优雅关闭；验证正常停机请用终端Ctrl+C或远程容器stop。
XML的Cannot find declaration错误应检查XML插件外部Schema下载设置；
POM沿用Maven官方声明，不通过关闭所有XML校验解决。
清理旧目录前应先移除Eclipse中的旧子工程引用，否则运行中的IDE可能重建旧.project文件和目录。
根工程名称使用zbagentwf。Eclipse工作区可能仍缓存旧工程引用：先选中工程按F5刷新；
如仍显示旧工程，按上述步骤移除引用后重新导入，再清理已核实仅含缓存和IDE元数据的旧目录。
只保留根工程；本地IDE元数据仍不提交到Git，不直接修改Eclipse工作区.metadata。

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
