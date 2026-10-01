"""使用临时 Git 仓库验证自动实现的权限、快照、评审和并发回推行为。

不调用真实模型、不接触远程仓库/凭据；验证日志仅为测试合成材料。
"""

import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch


SPEC = importlib.util.spec_from_file_location(
    "automation", Path(__file__).resolve().parents[1] / "scripts/codex_automation.py")
automation = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(automation)


class AutomationTest(unittest.TestCase):
    """每个测试拥有独立仓库、材料目录和本地 bare remote，保持可重复和无网络。"""

    def setUp(self):
        """创建工作流引入提交与一个后续 user 提交，保存原工作目录以隔离测试。"""
        self.temporary = tempfile.TemporaryDirectory(prefix="zb-codex-test-")
        self.root = Path(self.temporary.name)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        self.bundle = self.root / "bundle"
        self.bundle.mkdir()
        self.original = Path.cwd()
        os.chdir(self.repo)
        automation.git("init", "-b", "main")
        automation.git("config", "user.name", "Fixture")
        automation.git("config", "user.email", "fixture@example.invalid")
        self.put(automation.WORKFLOW, "fixture workflow\n")
        self.put(".github/codex/proposal.md", "只读 fixture 计划材料\n")
        self.put(automation.USER + "Port.java", "interface Port {}\n")
        self.put(".gitignore", "target/\n/config/agents.properties\n.env\n")
        automation.git("add", "--", ".github", "src", ".gitignore")
        automation.git("commit", "-m", "fixture: introduce workflow")
        self.base = automation.head()
        self.put(automation.USER + "Port.java", "interface Port { void run(); }\n")
        automation.git("add", "--", "src")
        automation.git("commit", "-m", "fixture: user contract")
        self.source = automation.head()
        automation.write_json(self.bundle / "context.json", {
            "base": self.base, "source": self.source, "branch": "main"})
        automation.freeze_ignored(self.bundle)
        self.write_review("plan")
        automation.write_json(self.bundle / "implementation.json", {
            "status": "complete", "summary": "合成候选", "blockers": []})
        (self.bundle / "verify.log").write_text("合成验证证据，不代表真实 Maven 验收\n")

    def tearDown(self):
        """恢复调用者目录，只清理本测试创建的临时目录。"""
        os.chdir(self.original)
        self.temporary.cleanup()

    def put(self, path, text):
        """在测试仓库生成一个普通 UTF-8 fixture 文件。"""
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    def write_review(self, phase, **changes):
        """生成严格格式的测试审核材料，供拒绝和快照绑定行为验证。"""
        result = {"phase": phase, "acceptance": "Accept", "can_implement": True,
                  "can_commit_push": phase == "result", "p0": 0, "p1": 0,
                  "snapshot_sha256": "", "summary": "合成审核证据",
                  "file_coverage": ["fixture 文件核查"]}
        if phase == "result":
            result["snapshot_sha256"] = automation.read_json(self.bundle / "snapshot.json")["sha256"]
        result.update(changes)
        automation.write_json(self.bundle / f"{phase}-review.json", result)

    def make_candidate(self):
        """保存一个获准实现文件为候选，供跨 checkout 和发布验证复用。"""
        self.put("src/main/java/com/kk24426/zbagentwf/agent/Impl.java", "class Impl {}\n")
        automation.candidate(self.bundle)

    def test_protected_and_traversal_paths(self):
        """user、治理、配置、生产 schema 和不规范路径不能进入候选。"""
        for path in [automation.USER + "Port.java", ".github/x.py", "AGENTS.md", "pom.xml",
                     "src/main/resources/db/table.sql", "src/test/AGENTS.md",
                     "src/test/.env", "src/test/private.key", "src/test/third-party.jar",
                     "src/main/java/com/kk24426/zbagentwf/agent/schema.sql",
                     "src/main/java/com/kk24426/zbagentwf/common/pom.xml",
                     "src/main/java/com/kk24426/zbagentwf/common/deploy.sh",
                     "src/test/../../AGENTS.md", "/src/test/x.py"]:
            with self.subTest(path=path):
                self.assertFalse(automation.allowed(path))

    def test_guard_rejects_user_change(self):
        """用户最新接口即使编译失败也必须保留原样。"""
        self.put(automation.USER + "Port.java", "interface Port {}\n")
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)

    def test_ignored_configuration_cannot_bypass_guard(self):
        """Git 状态不可见的运行配置仍必须阻断，不能依赖模型自己报告。"""
        self.put("config/agents.properties", "fixture runtime input\n")
        self.assertEqual(automation.changed_files(), [])
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)

    def test_checkout_cannot_start_with_hidden_inputs(self):
        """隔离 runner 禁止依赖不会出现在候选 tree 的既有本地配置。"""
        self.put(".env", "fixture local input\n")
        with self.assertRaises(ValueError):
            automation.freeze_ignored(self.bundle)

    def test_target_outputs_are_disposable_but_links_are_rejected(self):
        """普通 target 产物允许后续 clean 重建，链接不能进入宿主清理步骤。"""
        self.put("target/classes/Stale.class", "fixture stale output\n")
        self.assertEqual(automation.guard(self.bundle), [])
        Path("target/classes/Stale.class").unlink()
        Path("target/classes/link").symlink_to(self.root)
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)

    def test_guard_rejects_symlink_and_parent(self):
        """候选文件和父目录的链接不能把写回范围导向其它资产。"""
        Path("src/test").mkdir()
        Path("src/test/link.java").symlink_to(Path(automation.USER + "Port.java").resolve())
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)
        Path("src/test/link.java").unlink()
        Path("src/test/linked").symlink_to(self.root)
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)

    def test_guard_rejects_gitlink(self):
        """已有或暂存的子模块入口不能混入普通实现。"""
        automation.git("update-index", "--add", "--cacheinfo", "160000", self.source, "src/test/submodule")
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)

    def test_sensitive_and_binary_material(self):
        """秘密和二进制内容在打包前阻断，不输出秘密原文。"""
        self.put("src/test/Fixture.java", "key=" + "sk-" + "A" * 40)
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)
        Path("src/test/Fixture.java").write_bytes(b"binary\0data")
        with self.assertRaises(ValueError):
            automation.guard(self.bundle)

    def test_failed_review_material_is_scanned_before_upload(self):
        """失败审核的最终输出也不能不经扫描就作为 artifact 保存。"""
        (self.bundle / "failed-review.json").write_text("sk-" + "B" * 40)
        with self.assertRaises(ValueError):
            automation.scan_bundle(self.bundle)
        (self.bundle / "failed-review.json").unlink()
        automation.scan_bundle(self.bundle)

    def test_review_rejection_and_false_types(self):
        """拒绝、P1 和字符串冒充许可都不能通过 Plan 门禁。"""
        for changes in [{"acceptance": "Revise"}, {"p1": 1}, {"p0": False},
                        {"can_implement": "true"}, {"can_commit_push": True},
                        {"file_coverage": []}]:
            with self.subTest(changes=changes):
                self.write_review("plan", **changes)
                with self.assertRaises(ValueError):
                    automation.review(self.bundle, "plan")

    def test_result_binds_exact_snapshot(self):
        """接受结论必须绑定实际 staged 补丁，不能复用另一快照的许可。"""
        self.make_candidate()
        self.write_review("result", snapshot_sha256="0" * 64)
        with self.assertRaises(ValueError):
            automation.review(self.bundle, "result")
        self.write_review("result")
        automation.review(self.bundle, "result")

    def test_post_review_worktree_change_invalidates(self):
        """评审后未暂存变化和验证日志变化均使提交许可失效。"""
        self.make_candidate()
        self.put("src/test/New.java", "class New {}\n")
        with self.assertRaises(ValueError):
            automation.assert_snapshot(self.bundle)
        Path("src/test/New.java").unlink()
        (self.bundle / "verify.log").write_text("changed")
        with self.assertRaises(ValueError):
            automation.assert_snapshot(self.bundle)

    def test_rename_delete_roundtrip(self):
        """合法 rename/delete 与新增在新 checkout 重建为同一 tree 和完整状态。"""
        self.put("src/test/Old.java", "class Old {}\n")
        automation.git("add", "--", "src/test/Old.java")
        automation.git("commit", "-m", "fixture: baseline test")
        self.source = automation.head()
        automation.write_json(self.bundle / "context.json", {"source": self.source})
        Path("src/test/Old.java").rename("src/test/New.java")
        automation.candidate(self.bundle)
        other = self.root / "other"
        automation.git("clone", "--no-hardlinks", str(self.repo), str(other))
        os.chdir(other)
        automation.apply_candidate(self.bundle)
        automation.assert_snapshot(self.bundle)
        self.assertTrue(Path("src/test/New.java").is_file())
        self.assertFalse(Path("src/test/Old.java").exists())

    def test_patch_tampering_rejected(self):
        """损坏补丁不能应用到新 checkout。"""
        self.make_candidate()
        other = self.root / "other"
        automation.git("clone", str(self.repo), str(other))
        (self.bundle / "candidate.patch").write_text("tampered")
        os.chdir(other)
        with self.assertRaises(ValueError):
            automation.apply_candidate(self.bundle)

    def test_no_change_has_no_empty_commit(self):
        """契约已满足时明确结束，既不创建快照也不制造空提交。"""
        automation.write_json(self.bundle / "implementation.json", {
            "status": "no_change", "summary": "合成无需改动", "blockers": []})
        automation.candidate(self.bundle)
        self.assertEqual(automation.head(), self.source)
        self.assertFalse((self.bundle / "snapshot.json").exists())

    def test_cumulative_base_includes_queued_changes(self):
        """首轮和连续 push 累计从工作流引入提交读取用户净改动。"""
        self.put(automation.USER + "Another.java", "interface Another {}\n")
        automation.git("add", "--", "src")
        automation.git("commit", "-m", "fixture: second user push")
        self.assertEqual(automation.automatic_base(automation.head(), self.source), self.base)

    def test_packet_uses_exact_user_delta(self):
        """规划材料读取真实提交差异，外部材料生成不会污染源码工作区。"""
        event = self.root / "event.json"
        automation.write_json(event, {"before": self.base})
        with patch.dict(os.environ, {"GITHUB_SHA": self.source, "GITHUB_EVENT_PATH": str(event)}):
            automation.packet(self.bundle, "proposal")
        context = automation.read_json(self.bundle / "context.json")
        self.assertEqual(context["base"], self.base)
        self.assertEqual(context["source"], self.source)
        self.assertIn("void run();", (self.bundle / "user.diff").read_text())
        self.assertEqual(automation.git("status", "--porcelain"), b"")

    def test_manual_packet_rejects_revision_and_empty_delta(self):
        """手动范围必须为真实祖先完整 SHA，空 user 差异不启动模型。"""
        event = self.root / "event.json"
        for base in ["--help", self.source]:
            with self.subTest(base=base):
                automation.write_json(event, {"inputs": {"base_sha": base}})
                with patch.dict(os.environ, {"GITHUB_SHA": self.source, "GITHUB_EVENT_PATH": str(event)}):
                    with self.assertRaises(ValueError):
                        automation.packet(self.bundle, "proposal")

    def test_blocked_implementation_is_not_staged(self):
        """需用户决策时保留待审材料，不将部分实现暂存为可提交候选。"""
        self.put("src/test/Incomplete.java", "class Incomplete {}\n")
        automation.write_json(self.bundle / "implementation.json", {
            "status": "blocked", "summary": "缺少明确行为", "blockers": ["需要用户契约"]})
        with self.assertRaises(ValueError):
            automation.candidate(self.bundle)
        self.assertEqual(automation.git("diff", "--cached", "--name-only"), b"")

    def test_successful_publish_and_watermark(self):
        """独立 commit 常规回推到本地 bare remote，后续差异以该成功提交为水位。"""
        remote = self.root / "remote.git"
        automation.git("init", "--bare", str(remote))
        automation.git("remote", "add", "origin", str(remote))
        automation.git("push", "origin", "main")
        self.make_candidate()
        self.write_review("result")
        automation.publish(self.bundle)
        result = automation.head()
        self.assertEqual(automation.git("rev-parse", "HEAD^").decode().strip(), self.source)
        self.assertEqual(automation.automatic_base(result, self.source), result)
        self.assertIn(result, automation.git("ls-remote", "origin", "refs/heads/main").decode())

    def test_advanced_remote_stops_publish(self):
        """用户先推入新提交时，旧候选不创建实现 commit、不覆盖远端。"""
        remote = self.root / "remote.git"
        automation.git("init", "--bare", str(remote))
        automation.git("remote", "add", "origin", str(remote))
        automation.git("push", "origin", "main")
        other = self.root / "other"
        automation.git("clone", "--branch", "main", str(remote), str(other))
        subprocess.run(["git", "-C", str(other), "config", "user.name", "Fixture"], check=True)
        subprocess.run(["git", "-C", str(other), "config", "user.email", "fixture@example.invalid"], check=True)
        (other / "new.txt").write_text("new user push\n")
        for args in [["add", "new.txt"], ["commit", "-m", "fixture: newer user commit"],
                     ["push", "origin", "main"]]:
            subprocess.run(["git", "-C", str(other), *args], check=True, capture_output=True)
        self.make_candidate()
        self.write_review("result")
        with self.assertRaises(ValueError):
            automation.publish(self.bundle)
        self.assertEqual(automation.head(), self.source)


if __name__ == "__main__":
    unittest.main()
