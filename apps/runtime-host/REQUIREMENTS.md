# Runtime Host Requirements

## 当前范围

- 作为 CLI 进程入口和未来组合根，负责命令解析、输出与退出码映射。
- 当前只提供 `help`、`--help`、`version` 和 `--version` 内建命令，不承载业务逻辑。
- 正式分发物为 `target/zbagentwf-cli-${project.version}.jar`，通过 `java -jar` 调用。

## 验收基线

- 只依赖已允许的 `adapters`、`application` 和 `domain` 公开出口。
- 无参数、`help` 和 `--help` 输出帮助并返回 `0`；`version` 和 `--version` 输出 Manifest 版本并返回 `0`。
- 功能执行失败只向 stderr 输出脱敏错误并返回 `1`；命令或参数错误只向 stderr 输出稳定提示并返回 `2`。
- 命令区分大小写，未知命令不得回显原始输入；内建命令不接受额外参数。
- 启动、配置读取和其它副作用必须显式；新增协议、配置或业务命令仍须由用户选择或批准后实施。

## 分发边界

- Maven Shade 在 `package` 阶段替换 runtime-host 主产物；`original-zbagentwf-cli-*.jar` 只是构建中间物，不得作为正式分发物。
- Maven 坐标保持 `com.kk24426.zbagentwf:zbagentwf-runtime-host`，install/deploy 文件名仍遵循坐标；发布 POM 保留模块依赖。
- runtime-host 当前定位为 CLI-only 分发，不承诺 Maven 类库兼容性。

## 待用户确认

- 第一条业务命令及其 application 接口；
- 配置来源、启动/关闭语义和目标环境；
- 是否需要 CLI 之外的其它进程协议。
