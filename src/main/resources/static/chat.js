/* 单次请求页面：不存储输入或结果，不将响应当作 HTML。 */
"use strict";

const form = document.getElementById("chat-form");
const message = document.getElementById("chat-message");
const send = document.getElementById("chat-send");
const result = document.getElementById("chat-result");
const status = document.getElementById("chat-status");
const reply = document.getElementById("chat-reply");
const requestId = document.getElementById("chat-request-id");
let pending = false;

function refreshButton() {
  send.disabled = pending || message.value.trim().length === 0;
}

message.addEventListener("input", refreshButton);
refreshButton();

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (pending || !message.value.trim() || !form.reportValidity()) return;
  pending = true;
  refreshButton();
  send.textContent = "发送中…";
  result.dataset.state = "pending";
  result.setAttribute("aria-busy", "true");
  status.textContent = "正在处理";
  reply.textContent = "正在发送请求，请稍候。";
  requestId.textContent = "";
  try {
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: {"Content-Type": "application/json", "Accept": "application/json"},
      body: JSON.stringify({message: message.value}),
      signal: AbortSignal.timeout(30000)
    });
    const id = response.headers.get("X-Request-ID");
    if (id) requestId.textContent = `请求编号：${id}`;
    if (!response.ok) {
      // 只展示本地固定错误文字，不把服务端或代理的任意错误正文直接呈现给用户。
      const errors = {
        400: "请输入非空请求，长度不超过 4000 字符。",
        405: "请求方式不受支持，请刷新页面重试。",
        415: "请求格式不受支持，请刷新页面重试。",
        503: "Agent 尚未接入，或服务暂不可用。请稍后重试。"
      };
      status.textContent = "本次请求未完成";
      reply.textContent = errors[response.status] || "服务处理失败，请联系维护者并提供请求编号。";
      result.dataset.state = "error";
      return;
    }
    const data = await response.json();
    if (typeof data.reply !== "string") throw new Error("Invalid response");
    status.textContent = "已收到结果";
    reply.textContent = data.reply;
    result.dataset.state = "success";
  } catch (error) {
    status.textContent = "本次请求未完成";
    reply.textContent = error.name === "TimeoutError"
      ? "等待响应超时，请稍后重试。"
      : "连接失败或响应格式异常，请确认服务正常后重试。";
    result.dataset.state = "error";
  } finally {
    pending = false;
    send.textContent = "发送请求 ↗";
    result.setAttribute("aria-busy", "false");
    refreshButton();
  }
});
