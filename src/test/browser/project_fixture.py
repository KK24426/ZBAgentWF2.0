"""
创建日期：2026-09-27
更新日期：2026-09-27
做 成 者：zebiao
版    本：v0.1
功能概要：项目浏览器验收替身，代理真实页面；全部业务请求在本机模拟，不调用真实模型。

python -X utf8 src/test/browser/project_fixture.py --upstream-port <本机测试JAR端口>
仅绑定回环随机端口，启动打印端口。fixture:slow/partial/failure/confirm/unavailable/invalid
是合成测试标记；/fixture/stats 仅供测试核对请求次数和载荷。本文件不进入正式 JAR。
"""
import argparse
import copy
import http.client
import json
import re
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream-port", type=int, required=True)
    args = parser.parse_args()
    if not 1 <= args.upstream_port <= 65535:
        parser.error("upstream-port must be between 1 and 65535")
    projects, requests, partial = {}, [], set()
    lock = threading.Lock()

    def requirement(content, number):
        return {"userContent": content, "agentUnderstanding": "理解 <img src=x onerror=alert(1)> 日本語",
                "acceptanceCriteria": "检查实际产物", "userConfirmMsg": None,
                "tasks": [{"id": str(number), "content": content,
                           "acceptanceCriteria": "结果符合要求", "status": "PENDING", "result": None}]}

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass

        def send(self, status, value, head=False, raw=False):
            body = value.encode("utf-8") if raw else json.dumps(value, ensure_ascii=False).encode("utf-8")
            if status == 204:
                body = b""
            self.send_response(status)
            self.send_header("Content-Type", "application/json;charset=UTF-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Request-ID", "project-fixture-request")
            self.end_headers()
            if not head:
                self.wfile.write(body)

        def do_GET(self):
            if self.path == "/fixture/stats":
                with lock:
                    self.send(200, copy.deepcopy(requests))
                return
            if self.path.startswith("/api/projects"):
                with lock:
                    requests.append({"method": "GET", "path": self.path})
                    project = projects.get(self.path.removeprefix("/api/projects/"))
                    self.send(200 if project else 404, copy.deepcopy(project) if project else "private-error")
                return
            self.proxy("GET")

        def do_HEAD(self):
            self.proxy("HEAD")

        def proxy(self, method):
            # 只代理固定只读资源，绝不把 fixture 业务路径或写请求发给真实 Agent。
            if self.path not in ("/", "/index.html", "/projects", "/projects.html", "/app.css",
                                 "/projects.css", "/projects.js", "/messages.js", "/chat.js", "/favicon.svg"):
                self.send(404, "fixture route not found", head=method == "HEAD")
                return
            upstream = http.client.HTTPConnection("127.0.0.1", args.upstream_port, timeout=10)
            try:
                headers = {key: self.headers[key] for key in ("Accept-Language", "Cookie") if key in self.headers}
                upstream.request(method, self.path, headers=headers)
                response = upstream.getresponse()
                body = response.read()
                self.send_response(response.status)
                for key, value in response.getheaders():
                    if key.lower() not in {"transfer-encoding", "connection", "content-length"}:
                        self.send_header(key, value)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                if method != "HEAD":
                    self.wfile.write(body)
            finally:
                upstream.close()

        def do_POST(self):
            try:
                length = int(self.headers.get("Content-Length", "0"))
                if not 0 < length <= 1048576:
                    raise ValueError()
                data = json.loads(self.rfile.read(length))
                if not isinstance(data, dict):
                    raise ValueError()
            except (ValueError, json.JSONDecodeError):
                self.send(400, "private-invalid-input")
                return
            with lock:
                requests.append({"method": "POST", "path": self.path, "body": data})
            if self.path == "/api/chat":
                self.send(200, {"reply": "原样回复 <img src=x onerror=alert(1)>"})
                return
            content = data.get("content", "")
            if "fixture:unavailable" in content:
                self.send(503, "<script>private-server-error</script>")
                return
            if "fixture:slow" in content:
                time.sleep(3)
            if self.path == "/api/projects":
                with lock:
                    project_id = "fixture-" + str(len(projects) + 1)
                    project = {"projectId": project_id, "projectName": data.get("projectName"), "requirements": [requirement(content, 1)]}
                    projects[project_id] = project
                    self.send(201, copy.deepcopy(project))
                return
            route = re.fullmatch(r"/api/projects/([A-Za-z0-9-]+)/(requirements|prompts|tasks/execute)", self.path)
            if not route:
                self.send(404, "private-unknown-route")
                return
            project_id, action = route.groups()
            with lock:
                project = projects.get(project_id)
                if project is None:
                    self.send(404, "private-missing-project")
                    return
                if action == "prompts":
                    self.send(204, None)
                    return
                if action == "requirements":
                    project["requirements"].append(requirement(content, len(project["requirements"]) + 1))
                    self.send(200, "{invalid", raw=True) if "fixture:invalid" in content else self.send(200, copy.deepcopy(project))
                    return
                if data:
                    self.send(400, "execution body must be empty")
                    return
                for req in project["requirements"]:
                    for task in req["tasks"]:
                        if task["status"] in ("FAILED", "NEEDS_CONFIRMATION"):
                            self.send(200, copy.deepcopy(project))
                            return
                        if task["status"] != "PENDING":
                            continue
                        failed = "fixture:failure" in task["content"]
                        confirmation = "fixture:confirm" in task["content"]
                        task["status"] = "FAILED" if failed else "NEEDS_CONFIRMATION" if confirmation else "SUCCEEDED"
                        task["result"] = {"taskId": "exec-" + task["id"], "success": not failed and not confirmation,
                                          "errorMessage": "模拟失败" if failed else None, "tokenCount": 42,
                                          "summary": "结果 <script>alert(1)</script>", "confirmationRequired": confirmation,
                                          "confirmationMessage": "请确认目标" if confirmation else None}
                        if "fixture:partial" in task["content"] and project_id not in partial:
                            partial.add(project_id)
                            self.send(503, "private-partial-execution")
                            return
                        if failed or confirmation:
                            self.send(200, copy.deepcopy(project))
                            return
                self.send(200, copy.deepcopy(project))

    with ThreadingHTTPServer(("127.0.0.1", 0), Handler) as server:
        print(json.dumps({"port": server.server_port}), flush=True)
        server.serve_forever()


if __name__ == "__main__":
    main()
