"""
创建日期：2026-09-27
更新日期：2026-09-27
做 成 者：zebiao
版    本：v0.1
功能概要：仅用于浏览器验收的回环HTTP替身，代理真实首页并模拟延迟/超时/安全错误。

用法：python -X utf8 src/test/browser/localization_fixture.py --upstream-port <本机测试JAR端口>
绑定127.0.0.1随机端口；启动打印端口。输入fixture:delayed、fixture:timeout、fixture:invalid，
其它输入返回模拟503。此文件不进入正式JAR，不调用真实Agent，也不修改正式服务。
"""

import argparse
import http.client
import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream-port", type=int, required=True)
    arguments = parser.parse_args()
    if not 1 <= arguments.upstream_port <= 65535:
        parser.error("upstream-port must be between 1 and 65535")
    sequence = 0
    lock = threading.Lock()

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass  # 不记录原始URL或正文。

        def do_GET(self):
            self.proxy("GET")

        def do_HEAD(self):
            self.proxy("HEAD")

        def proxy(self, method):
            upstream = http.client.HTTPConnection("127.0.0.1", arguments.upstream_port, timeout=10)
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
            nonlocal sequence
            if self.path != "/api/chat":
                self.send_error(404)
                return
            content = json.loads(self.rfile.read(int(self.headers.get("Content-Length", "0"))))
            scenario = content.get("message")
            with lock:
                sequence += 1
                execution_id = "fixture-request-" + str(sequence)
            print(execution_id, flush=True)
            if scenario == "fixture:delayed":
                time.sleep(8)
                status = 200
                body = json.dumps({"reply": "原样回复 <img src=x onerror=alert(1)> 日本語"},
                                  ensure_ascii=False).encode("utf-8")
            elif scenario == "fixture:timeout":
                time.sleep(35)
                status, body = 200, b'{"reply":"late fixture"}'
            elif scenario == "fixture:invalid":
                status, body = 200, b'{"reply":123}'
            else:
                status, body = 503, b"<script>private-proxy-error</script>"
            try:
                self.send_response(status)
                self.send_header("Content-Type", "application/json;charset=UTF-8")
                self.send_header("Content-Length", str(len(body)))
                self.send_header("X-Request-ID", execution_id)
                self.end_headers()
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                pass  # 浏览器超时断开是本替身的预期结果。

    with ThreadingHTTPServer(("127.0.0.1", 0), Handler) as server:
        print(json.dumps({"port": server.server_port}), flush=True)
        server.serve_forever()


if __name__ == "__main__":
    main()
