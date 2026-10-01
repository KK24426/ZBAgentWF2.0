#!/usr/bin/env python3
"""为 user 提交的自动实现生成材料、核验评审和快照，并常规写回 main。

仅由 workflow 的宿主步骤调用；实现模型调用前须复制到 RUNNER_TEMP，
避免候选代码改变审核或提交程序。不读取凭据，不执行任意 shell 字符串。
"""

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys


# 与 workflow push 路径一致；该目录只能读取用户的已提交契约。
USER = "src/main/java/com/kk24426/zbagentwf/user/"
WORKFLOW = ".github/workflows/codex-user-implementation.yml"
# 固定 bot 身份、消息和源提交 trailer 共同识别累计差异的成功水位。
COMMIT_MESSAGE = "feat: implement user changes with Codex"
BOT_EMAIL = "41898282+github-actions[bot]@users.noreply.github.com"
# 外层路径白名单不授予新的业务权限，语义授权仍由独立评审核查。
ALLOWED_PREFIXES = (
    "src/main/java/com/kk24426/zbagentwf/agent/",
    "src/main/java/com/kk24426/zbagentwf/common/",
    "src/test/", "docs/contracts/", "docs/code-map/",
)
# 仅阻断可识别形态，不声称能证明任意文本中没有秘密。
SECRET = re.compile(
    rb"-----BEGIN (?:[A-Z ]+ )?PRIVATE KEY-----|"
    rb"\b(?:sk-[A-Za-z0-9_-]{24,}|gh[pousr]_[A-Za-z0-9]{30,}|"
    rb"github_pat_[A-Za-z0-9_]{30,}|AKIA[A-Z0-9]{16})\b|"
    rb"(?i:(?:api[_-]?key|password|secret|access[_-]?token))"
    rb"\s*[:=]\s*[\"']?[A-Za-z0-9+/=_-]{32,}"
)


def git(*args, data=None):
    """执行固定 Git 子命令并返回字节；失败不打印可能含秘密的子进程输出。"""
    result = subprocess.run(["git", *args], input=data, capture_output=True)
    if result.returncode:
        raise ValueError(f"Git 操作失败：{args[0]}，退出码 {result.returncode}")
    return result.stdout


def scan(data):
    """阻断常见凭据形态；此启发式不能替代用户的公开仓库资产检查。"""
    if SECRET.search(data):
        raise ValueError("材料包含疑似敏感信息；已停止，不输出匹配内容")


def scan_bundle(bundle):
    """失败材料同样先扫描再上传；拒绝链接和疑似秘密，不把阻断记录直接打包。"""
    if not bundle.is_dir():
        raise ValueError("没有可上传的任务材料")
    for path in bundle.rglob("*"):
        if path.is_symlink():
            raise ValueError("审核材料不允许链接")
        if path.is_file():
            scan(path.read_bytes())


def read_json(path):
    """读取结构化评审或快照材料，非法或缺失材料直接失败。"""
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path, value):
    """仅在宿主指定的材料目录保存 UTF-8 JSON，不向源码添加审核记录。"""
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def output(name, value):
    """写入 GitHub 单行 job output；本地验证时不要求 GITHUB_OUTPUT。"""
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
            stream.write(f"{name}={value}\n")


def head():
    """返回实际 HEAD，用于拒绝模型提交、切换基准和任务材料串用。"""
    return git("rev-parse", "HEAD").decode().strip()


def valid_sha(value):
    """只接受完整提交 SHA，避免把外部输入解释成 Git option 或 revision 表达式。"""
    if not re.fullmatch(r"[0-9a-f]{40}", value) or value == "0" * 40:
        raise ValueError("基准必须是非零的完整提交 SHA")
    git("cat-file", "-e", value + "^{commit}")
    return value


def automatic_base(source, event_before):
    """累计尚未完成的用户净改动，避免 concurrency 替换排队任务时漏掉接口。

    最近一次本工作流的 bot 实现提交作为水位；首次运行以工作流引入提交
    为锚点。中间契约以 source 最新状态为准，不逐个实现已撤销的方法。
    """
    for commit in git("rev-list", source).decode().splitlines():
        info = git("show", "-s", "--format=%ae%n%s%n%B", commit).decode()
        lines = info.splitlines()
        if len(lines) >= 2 and lines[0] == BOT_EMAIL and lines[1] == COMMIT_MESSAGE:
            match = re.search(r"^Codex-Source: ([0-9a-f]{40})$", info, re.MULTILINE)
            if match and git("rev-parse", commit + "^").decode().strip() == match[1]:
                return commit
    added = git("log", "--diff-filter=A", "--format=%H", source, "--", WORKFLOW).decode().splitlines()
    return added[-1] if added and added[-1] != source else valid_sha(event_before)


def allowed(path):
    """限制候选职责和文件名；治理、user、构建、生产资源始终不在自动写回范围。"""
    parts = PurePosixPath(path).parts
    name = PurePosixPath(path).name
    prohibited_asset = (
        name.startswith(".env") and name != ".env.example"
        or PurePosixPath(path).suffix.lower() in {".pem", ".key", ".pfx", ".p12", ".class", ".jar", ".war", ".ear"}
    )
    if (not re.fullmatch(r"[A-Za-z0-9_./-]+", path) or ".." in parts
            or path.startswith("/") or "AGENTS.md" in parts or prohibited_asset):
        return False
    if path.startswith(ALLOWED_PREFIXES[:2]):
        return PurePosixPath(path).suffix == ".java"
    return path == "REQUIREMENTS.md" or path.startswith(ALLOWED_PREFIXES)


def changed_files():
    """将 rename 视为删除和新增，读取 staged、unstaged 和未跟踪文件的并集。"""
    changed = git("diff", "--name-only", "--no-renames", "-z")
    changed += git("diff", "--cached", "--name-only", "--no-renames", "-z")
    unknown = git("ls-files", "--others", "--exclude-standard", "-z")
    return sorted({p.decode() for p in (changed + unknown).split(b"\0") if p})


def ignored_inputs():
    """识别不会随候选 tree 重建的忽略输入，不输出或打包其内容。

    target 中的普通构建输出由宿主 clean verify 删除后重建；其中的链接也
    拒绝，避免清理作用于工作区外。其它忽略文件必须没有新增、改删或依赖。
    """
    state = {}
    for name in git("ls-files", "--others", "--ignored", "--exclude-standard", "-z").split(b"\0"):
        if not name:
            continue
        path = Path(name.decode())
        if any(p.is_symlink() for p in [path, *path.parents]):
            raise ValueError("忽略文件或构建产物包含链接，禁止继续构建")
        if path.as_posix().startswith("target/"):
            continue
        if not path.is_file():
            raise ValueError("忽略输入必须是普通文件")
        state[path.as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    return state


def freeze_ignored(bundle):
    """模型开始前保存忽略输入基线；新 runner 不允许依赖不会被重建的本地配置。"""
    baseline = ignored_inputs()
    if baseline:
        raise ValueError("隔离 checkout 存在构建输出以外的忽略输入，不能重建精确候选")
    write_json(bundle / "ignored-inputs.json", baseline)


def guard(bundle):
    """在构建和暂存前检查目标、越权、链接及敏感文件；不静默排除违规文件。"""
    context = read_json(bundle / "context.json")
    if head() != context["source"]:
        raise ValueError("HEAD 偏离用户提交，禁止提交或验证候选")
    if ignored_inputs() != read_json(bundle / "ignored-inputs.json"):
        raise ValueError("实现改动了忽略输入；配置或临时依赖不能绕过审核")
    paths = changed_files()
    for path in paths:
        if not allowed(path):
            raise ValueError(f"候选超出自动实现边界：{path}")
        # 删除已存在的链接或子模块同样越界，不仅检查新增文件的磁盘类型。
        for listing in (git("ls-tree", "HEAD", "--", path),
                        git("ls-files", "--stage", "--", path)):
            for entry in listing.splitlines():
                if entry.split(b" ", 1)[0] not in {b"100644", b"100755"}:
                    raise ValueError(f"自动候选不允许 symlink 或 gitlink：{path}")
        target = Path(path)
        if any(p.is_symlink() for p in [target, *target.parents]):
            raise ValueError(f"不允许链接文件或链接父目录：{path}")
        if target.exists():
            if not target.is_file():
                raise ValueError(f"候选必须是普通文件：{path}")
            data = target.read_bytes()
            if b"\0" in data:
                raise ValueError(f"自动提交不接受二进制文件：{path}")
            data.decode("utf-8")
            scan(data)
    scan(git("diff", "--full-index", "--binary", "HEAD"))
    return paths


def review(bundle, phase):
    """严格解释真实独立评审，拒绝错误类型、缺字段、拒绝结论和快照串用。"""
    result = read_json(bundle / f"{phase}-review.json")
    required = {"phase", "acceptance", "can_implement", "can_commit_push", "p0", "p1",
                "snapshot_sha256", "summary", "file_coverage"}
    if set(result) != required or result["phase"] != phase:
        raise ValueError("审核记录格式或阶段错误")
    if (result["acceptance"] != "Accept"
            or type(result["p0"]) is not int or result["p0"] != 0
            or type(result["p1"]) is not int or result["p1"] != 0
            or not isinstance(result["summary"], str) or not result["summary"].strip()
            or not isinstance(result["file_coverage"], list) or not result["file_coverage"]
            or any(not isinstance(item, str) or not item.strip() for item in result["file_coverage"])):
        raise ValueError("审核未接受、存在 P0/P1 或缺少核查证据")
    if type(result["can_implement"]) is not bool or type(result["can_commit_push"]) is not bool:
        raise ValueError("审核许可必须是真实布尔值")
    if phase == "plan" and (result["can_implement"] is not True
                            or result["can_commit_push"] is not False
                            or result["snapshot_sha256"] != ""):
        raise ValueError("Plan 未获得实施许可或提前签发提交许可")
    if phase == "result":
        manifest = read_json(bundle / "snapshot.json")
        if (result["can_implement"] is not True or result["can_commit_push"] is not True
                or result["snapshot_sha256"] != manifest["sha256"]):
            raise ValueError("Result 未获得提交许可或审核的快照不一致")
    scan(json.dumps(result).encode())
    return result


def packet(bundle, phase):
    """生成外部审核材料和提示词；模型不得把 commit 内容当作权限指令。"""
    bundle.mkdir(parents=True, exist_ok=True)
    if phase == "proposal":
        event = read_json(Path(os.environ["GITHUB_EVENT_PATH"]))
        source = valid_sha(os.environ["GITHUB_SHA"])
        manual = event.get("inputs", {}).get("base_sha")
        base = valid_sha(manual) if manual else automatic_base(source, event.get("before", ""))
        git("merge-base", "--is-ancestor", base, source)
        if head() != source or git("status", "--porcelain", "--untracked-files=all"):
            raise ValueError("规划需要触发提交上的干净工作区")
        paths = git("diff", "--name-only", "--no-renames", base, source, "--", USER).decode().splitlines()
        if not paths:
            raise ValueError("指定范围没有 user 净改动，不启动模型")
        delta = git("diff", "--full-index", base, source, "--", USER)
        scan(delta)
        context = {"base": base, "source": source, "branch": "main", "user_files": paths,
                   "authorization": "用户已要求 push 后用 GitHub Actions 自动实现 user 改动；仅适配明确的新契约。"}
        write_json(bundle / "context.json", context)
        (bundle / "user.diff").write_bytes(delta)
    else:
        context = read_json(bundle / "context.json")
        if context["source"] != head():
            raise ValueError("任务材料不属于当前源提交")
        if phase == "implement":
            review(bundle, "plan")
            freeze_ignored(bundle)
    template = Path(f".github/codex/{phase}.md").read_text(encoding="utf-8")
    prompt = template + "\n\n材料目录（只读取，不修改）：" + str(bundle.resolve())
    prompt += "\n任务上下文：\n" + json.dumps(context, ensure_ascii=False)
    if phase == "result-review":
        prompt += "\n精确候选快照：\n" + json.dumps(read_json(bundle / "snapshot.json"), ensure_ascii=False)
    (bundle / f"{phase}-prompt.md").write_text(prompt + "\n", encoding="utf-8")


def snapshot_values():
    """对完整 staged 补丁、tree、文件清单及状态绑定审核，不只散列开发摘要。"""
    patch = git("diff", "--cached", "--full-index", "--binary")
    return patch, {"source": head(), "sha256": hashlib.sha256(patch).hexdigest(),
                   "git_patch_hash": git("hash-object", "--stdin", data=patch).decode().strip(),
                   "tree": git("write-tree").decode().strip(),
                   "files": sorted(p.decode() for p in git("diff", "--cached", "--name-only", "-z").split(b"\0") if p),
                   "status": git("status", "--porcelain=v1", "--untracked-files=all").decode()}


def candidate(bundle):
    """读取实现结论、验证范围并显式 stage；构建证据由前序宿主 verify 步骤提供。"""
    review(bundle, "plan")
    report = read_json(bundle / "implementation.json")
    if (report.get("status") not in {"complete", "no_change"} or report.get("blockers") != []
            or not isinstance(report.get("summary"), str) or not report["summary"].strip()):
        raise ValueError("实现未完成或需用户决定；停止自动回推")
    paths = guard(bundle)
    if git("diff", "--cached", "--name-only"):
        raise ValueError("实现模型不得提前 stage；候选由宿主统一暂存")
    output("changed", "true" if paths else "false")
    if not paths:
        if report["status"] != "no_change":
            raise ValueError("空候选必须明确说明无需改动，不制造空提交")
        return
    if report["status"] != "complete":
        raise ValueError("no_change 结论与候选修改不一致")
    git("add", "--", *paths)
    for entry in git("ls-files", "--stage", "-z", "--", *paths).split(b"\0"):
        if entry and entry.split(b" ", 1)[0] not in {b"100644", b"100755"}:
            raise ValueError("自动提交不允许 symlink 或 gitlink")
    patch, manifest = snapshot_values()
    scan(patch)
    manifest["verify_log_sha256"] = hashlib.sha256((bundle / "verify.log").read_bytes()).hexdigest()
    manifest["ignored_inputs_sha256"] = hashlib.sha256((bundle / "ignored-inputs.json").read_bytes()).hexdigest()
    manifest["validation"] = "Java 26 / Maven Wrapper: ./mvnw -B clean verify 成功；MySQL 环境验收未启用"
    (bundle / "candidate.patch").write_bytes(patch)
    write_json(bundle / "snapshot.json", manifest)


def assert_snapshot(bundle):
    """评审后和提交前重新确认没有新增、漏暂存或已审快照变动。"""
    guard(bundle)
    _, actual = snapshot_values()
    expected = read_json(bundle / "snapshot.json")
    if any(expected[key] != value for key, value in actual.items()):
        raise ValueError("候选快照或工作区改变，原审核失效")
    if expected["verify_log_sha256"] != hashlib.sha256((bundle / "verify.log").read_bytes()).hexdigest():
        raise ValueError("验证证据改变")
    if expected["ignored_inputs_sha256"] != hashlib.sha256((bundle / "ignored-inputs.json").read_bytes()).hexdigest():
        raise ValueError("忽略输入基线改变")


def apply_candidate(bundle):
    """在源提交的新 checkout 重建同一个 staged snapshot，审核和发布不得重新生成代码。"""
    manifest = read_json(bundle / "snapshot.json")
    if head() != manifest["source"] or git("status", "--porcelain", "--untracked-files=all"):
        raise ValueError("只能在源提交的干净 checkout 应用候选")
    patch = (bundle / "candidate.patch").read_bytes()
    if hashlib.sha256(patch).hexdigest() != manifest["sha256"]:
        raise ValueError("候选补丁损坏或被替换")
    if not manifest["files"] or any(not allowed(path) for path in manifest["files"]):
        raise ValueError("候选文件清单越权")
    git("apply", "--index", "--", str((bundle / "candidate.patch").resolve()))
    assert_snapshot(bundle)


def check_remote(source, remote_sha):
    """仅在远端仍为本次源提交时允许回推，新用户提交优先，禁止自动合并或覆盖。"""
    if remote_sha != source:
        raise ValueError("main 已有新提交；停止回推，最新任务将处理累计 user 改动")


def publish(bundle):
    """由唯一有 contents:write 的无模型 job 创建独立实现提交并普通 fast-forward push。"""
    review(bundle, "plan")
    review(bundle, "result")
    assert_snapshot(bundle)
    source = read_json(bundle / "context.json")["source"]
    remote = git("ls-remote", "origin", "refs/heads/main").decode().split()
    check_remote(source, remote[0] if remote else "")
    git("config", "user.name", "github-actions[bot]")
    git("config", "user.email", BOT_EMAIL)
    git("-c", "core.hooksPath=/dev/null", "commit", "-m", COMMIT_MESSAGE,
        "-m", "Codex-Source: " + source)
    manifest = read_json(bundle / "snapshot.json")
    if git("rev-parse", "HEAD^{tree}").decode().strip() != manifest["tree"]:
        raise ValueError("提交 tree 与已审候选不一致")
    if git("status", "--porcelain", "--untracked-files=all"):
        raise ValueError("提交后工作区不干净")
    git("push", "origin", "HEAD:refs/heads/main")
    remote = git("ls-remote", "origin", "refs/heads/main").decode().split()
    if not remote or remote[0] != head():
        raise ValueError("回推后的远端 commit 核验失败")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
            stream.write(f"已将通过双阶段审核的实现提交 `{head()}` 写回 `main`。\n")


def main():
    """解析受控宿主命令；任何失败都使 job 终止，不自动重试模型或 Git 写入。"""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["packet", "review", "guard", "candidate", "apply", "assert", "publish", "scan-bundle"])
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--phase", choices=["proposal", "plan-review", "implement", "result-review", "plan", "result"])
    args = parser.parse_args()
    if args.command == "packet":
        packet(args.bundle, args.phase)
    elif args.command == "review":
        review(args.bundle, args.phase)
    else:
        {"guard": guard, "candidate": candidate, "apply": apply_candidate,
         "assert": assert_snapshot, "publish": publish, "scan-bundle": scan_bundle}[args.command](args.bundle)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, UnicodeError) as error:
        print(f"自动实现已停止：{error}", file=sys.stderr)
        sys.exit(1)
