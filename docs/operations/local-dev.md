<!--
 * 创建日期：2026-08-09
 * 更新日期：2026-08-19
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：说明 Java 26 Maven 工程、CLI 构建和验证命令。
 -->

# Local Development

## 环境

- JDK 26；
- Windows 使用 `mvnw.cmd`；
- macOS/Linux 使用 `./mvnw`，首次下载还需要 `unzip`，以及 `sha256sum` 或 `shasum`。

Maven Wrapper 固定 Maven 3.9.16。首次执行会从 Maven Central 下载 Maven，下载内容进入用户 Maven 缓存，不进入仓库。

需要隔离验证缓存时，必须同时隔离 Wrapper distribution 和 Maven artifact repository。`MAVEN_USER_HOME` 只控制 Wrapper distribution，不能替代 `-Dmaven.repo.local`：

```powershell
$systemTemp = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd([System.IO.Path]::DirectorySeparatorChar)
$verificationRoot = Join-Path $systemTemp ("zbagentwf2-verify-" + [guid]::NewGuid().ToString("N"))
$resolvedRoot = [System.IO.Path]::GetFullPath($verificationRoot)
$requiredPrefix = $systemTemp + [System.IO.Path]::DirectorySeparatorChar
if (-not $resolvedRoot.StartsWith($requiredPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "临时验证目录超出系统临时目录边界：$resolvedRoot"
}
$hadMavenUserHome = Test-Path Env:MAVEN_USER_HOME
$originalMavenUserHome = $env:MAVEN_USER_HOME

try {
    $env:MAVEN_USER_HOME = Join-Path $resolvedRoot "wrapper-home"
    $artifactRepository = Join-Path $resolvedRoot "repository"
    .\mvnw.cmd -V "-Dmaven.repo.local=$artifactRepository" verify
} finally {
    if ($hadMavenUserHome) {
        $env:MAVEN_USER_HOME = $originalMavenUserHome
    } else {
        Remove-Item Env:MAVEN_USER_HOME -ErrorAction SilentlyContinue
    }
    if ((Test-Path -LiteralPath $resolvedRoot) -and
        $resolvedRoot.StartsWith($requiredPrefix, [System.StringComparison]::OrdinalIgnoreCase) -and
        [System.IO.Path]::GetFileName($resolvedRoot).StartsWith("zbagentwf2-verify-")) {
        [System.IO.Directory]::Delete($resolvedRoot, $true)
    }
}
```

当前只在 Windows 上完成了 Wrapper 构建验证；macOS/Linux 的命令和前置条件已记录，但尚未完成对应环境的首次下载 smoke。

## 命令

```powershell
.\mvnw.cmd verify
```

指定模块及其依赖：

```powershell
.\mvnw.cmd -pl apps/runtime-host -am test
```

`verify` 会运行 runtime-host CLI 单元测试并生成正式分发物：

```text
apps/runtime-host/target/zbagentwf-cli-0.1.0-SNAPSHOT.jar
```

实际 JAR smoke：

```powershell
java -jar .\apps\runtime-host\target\zbagentwf-cli-0.1.0-SNAPSHOT.jar help
java -jar .\apps\runtime-host\target\zbagentwf-cli-0.1.0-SNAPSHOT.jar version
```

当前没有业务测试；reactor 仍为父工程加四个子模块，共 5/5。新增业务功能后必须补充对应测试。

## 环境边界

当前没有数据库、Agent provider、远程服务或部署环境。任何 migration、写库、远程调用和真实 Agent 执行前，必须先由用户确认目标与配置来源。
