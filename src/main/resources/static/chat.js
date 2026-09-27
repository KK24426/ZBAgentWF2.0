/* 页面和聊天只通过消息 key 渲染固定文字；只持久化语言，不存储输入或结果。 */
(() => {
  "use strict";
  const {msg} = window.ZbMessages;
  const form = document.getElementById("chat-form");
  const message = document.getElementById("chat-message");
  const send = document.getElementById("chat-send");
  const sendLabel = document.getElementById("chat-send-label");
  const result = document.getElementById("chat-result");
  const status = document.getElementById("chat-status");
  const reply = document.getElementById("chat-reply");
  const requestId = document.getElementById("chat-request-id");
  let pending = false;
  let statusKey = "chat.status.waiting";
  let replyKey = "chat.initial";
  let replyText = "";
  let id = "";

  function refreshButton() {
    send.disabled = pending || message.value.trim().length === 0;
    sendLabel.textContent = msg(pending ? "chat.sending" : "chat.send");
  }

  function renderState() {
    status.textContent = msg(statusKey);
    reply.textContent = replyKey ? msg(replyKey) : replyText;
    requestId.textContent = id ? msg("chat.requestId", id) : "";
    result.setAttribute("aria-busy", String(pending));
    refreshButton();
  }

  message.addEventListener("input", refreshButton);
  window.ZbMessages.onChange(renderState);
  renderState();

  form.addEventListener("submit", async event => {
    event.preventDefault();
    if (pending) return;
    // 自有校验消息随选择的语言变化，避免原生验证弹窗沿用浏览器语言。
    if (!message.value.trim() || message.value.length > 4000) {
      statusKey = "chat.status.failed";
      replyKey = !message.value.trim() ? "chat.validation.required" : "chat.validation.tooLong";
      id = "";
      result.dataset.state = "error";
      renderState();
      return;
    }
    pending = true;
    statusKey = "chat.status.pending";
    replyKey = "chat.pending";
    id = "";
    result.dataset.state = "pending";
    renderState();
    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: {"Content-Type": "application/json", "Accept": "application/json"},
        body: JSON.stringify({message: message.value}),
        signal: AbortSignal.timeout(30000)
      });
      id = response.headers.get("X-Request-ID") || "";
      if (!response.ok) {
        // 保持原边界：不直接展示来自服务端或代理的任意错误正文。
        const errors = {400: "chat.error.400", 405: "chat.error.405",
          415: "chat.error.415", 503: "chat.error.503"};
        statusKey = "chat.status.failed";
        replyKey = errors[response.status] || "chat.error.generic";
        result.dataset.state = "error";
        return;
      }
      const data = await response.json();
      if (typeof data.reply !== "string") throw new Error("Invalid response");
      statusKey = "chat.status.success";
      replyKey = null;
      replyText = data.reply;
      result.dataset.state = "success";
    } catch (error) {
      statusKey = "chat.status.failed";
      replyKey = error.name === "TimeoutError" ? "chat.error.timeout" : "chat.error.connection";
      result.dataset.state = "error";
    } finally {
      pending = false;
      renderState();
    }
  });
})();
