# 远程测试环境

当前任务获准在指定远程测试服务器启动独立Web服务，并在现有MySQL实例中新建专用zbagentwf_test库和受限账号。下文只给占位符，不保存真实连接或登录材料。
不得以测试为由改动其它项目的数据、账号、容器、端口、防火墙或代理配置。

## 部署约定

- 服务器任务目录：`$HOME/zbagentwf-test`，仅当前部署用户可访问。
- `releases/<SHA256>.jar`：上传后核对校验值的Web产物；不覆盖其它项目。
- `runtime.env`：权限600，保存mysql profile、数据源环境变量、SERVER_ADDRESS=127.0.0.1、SERVER_PORT=8080、ZB_LOG_DIR=/app/logs，以及必填的 ZB_PROJECT_ROOT；内容不进入日志或Git。
- `logs/<runId>/application.log`：持久日志，自动滚动但不自动删除；监控磁盘空间并由用户决定保留策略。
- 容器名`zbagentwf-web-test`；Java26官方镜像固定digest；非root、内存512MB、CPU1核、只读根文件系统、临时/tmp、只读JAR挂载及单独可写logs。
- 使用host网络但Web显式绑定127.0.0.1:8080；不映射公网端口。数据库仍使用服务器的回环3306。
- 重启策略unless-stopped；Spring关闭窗口20秒，容器停止窗口30秒。

当前运行的是测试服务，无登录保护。未来开放外网、配置TLS/代理/鉴权、业务schema或migration，需另行批准。
采用新增项目配置契约的版本时，部署前必须将 ZB_PROJECT_ROOT 设置为明确的容器内项目根目录；缺失或非法会启动失败。初始化只读取配置，不创建目录。本次代码修改不执行部署，也不新增目录挂载或写权限；实际项目目录创建留待后续任务。

## 从开发机访问

将占位符替换为用户提供的服务器、用户名及仓库外私钥路径；首次连接先核实服务器指纹，不禁用主机身份校验。

```powershell
ssh -N -i "PATH_TO_PRIVATE_KEY" -o StrictHostKeyChecking=yes -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -L 127.0.0.1:18080:127.0.0.1:8080 -L 127.0.0.1:13306:127.0.0.1:3306 USER@TEST_HOST
```

保持此终端运行，浏览器打开 http://127.0.0.1:18080/；关闭隧道终端只断开访问，不停止服务器。
如端口被占用先确认占用进程，不直接终止未知进程；改用空闲本地端口，并同步浏览器或测试配置。
数据库验证前确认隧道成功且对应端口由该SSH进程监听，避免误连本地其它数据库。

MySQL测试使用`ZB_TEST_DB_URL=jdbc:mysql://127.0.0.1:13306/zbagentwf_test`及独立`ZB_TEST_DB_USERNAME/PASSWORD`。
凭据由获授权的安全配置途径提供，不粘贴到仓库、命令参数或日志；测试后恢复/移除临时环境变量。
运行`mvnw.cmd -Pmysql-it verify`，仅创建并删除随机测试表，验证CRUD、中文、事务提交和回滚。
账号只具有此库SELECT/INSERT/UPDATE/DELETE/CREATE/DROP，无全局权限；不要在此技术测试库保存真实业务数据。

## 查看状态与停止

以下在已批准的服务器运行，只针对本项目容器：

```text
docker ps --filter name=^/zbagentwf-web-test$
docker logs --tail 100 zbagentwf-web-test
docker stop -t 30 zbagentwf-web-test
docker start zbagentwf-web-test
```

日志虽已做已知字段脱敏，分享前仍需检查。不要输出完整docker inspect（含环境凭据）。
正常SIGTERM停止的Java进程可能返回143；需同时核对非OOM、优雅关闭完成和连接池关闭日志，不能只看数字。
失败时停止本项目新容器并保留排错证据，不自动删除数据库、账号或其它项目资源。
更新部署先确认新JAR校验值、当前容器归属和回退版本，不能用宽泛的容器删除或目录清理命令。
