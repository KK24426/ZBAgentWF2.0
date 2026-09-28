/* 项目页面只消费已有 HTTP 快照；不持久化业务数据，不自动重试有副作用的请求。 */
(() => {
  "use strict";
  const {msg} = window.ZbMessages;
  const byId = id => document.getElementById(id);
  const statusKeys = {PENDING: "project.task.pending", RUNNING: "project.task.running",
    SUCCEEDED: "project.task.succeeded", FAILED: "project.task.failed", NEEDS_CONFIRMATION: "project.task.confirmation"};
  const goal = byId("project-goal"), lookup = byId("project-lookup-id");
  const requirement = byId("project-requirement"), prompt = byId("project-prompt");
  let current = null;
  let busy = false;
  let needsRefresh = false;
  let noticeKey = "project.status.ready";
  let noticeState = "ready";
  let requestId = "";
  let translated = [];
  const modelTranslated = [];

  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  function localized(tag, key, args = [], className, collection = translated) {
    const node = element(tag, msg(key, ...args), className);
    collection.push({node, key, args});
    return node;
  }
  function field(parent, key, value) {
    if (value === null || value === undefined || value === "") return;
    const block = element("div", undefined, "project-field");
    block.append(localized("h4", key), element("p", String(value)));
    parent.append(block);
  }
  function tasks() { return current ? current.requirements.flatMap(r => r.tasks) : []; }
  function attention() { return tasks().some(t => t.status === "FAILED" || t.status === "NEEDS_CONFIRMATION"); }
  function validId(value) { return typeof value === "string" && /^[A-Za-z0-9-]+$/.test(value); }
  function text(value) { return typeof value === "string"; }
  function nullableText(value) { return value === null || text(value); }

  // 仅接受已有响应结构，解析失败不能把可能完成的写操作当作未执行。
  function snapshot(value, expectedId) {
    if (!value || !validId(value.projectId) || expectedId && value.projectId !== expectedId
        || value.projectName !== undefined && !nullableText(value.projectName) || !Array.isArray(value.requirements)) throw new Error("Invalid project response");
    for (const r of value.requirements) {
      if (!r || !text(r.userContent) || !nullableText(r.agentUnderstanding) || !nullableText(r.acceptanceCriteria)
          || !nullableText(r.userConfirmMsg) || !Array.isArray(r.tasks)) throw new Error("Invalid requirement");
      for (const t of r.tasks) {
        if (!t || !text(t.id) || !text(t.content) || !nullableText(t.acceptanceCriteria)
            || !Object.hasOwn(statusKeys, t.status)) throw new Error("Invalid task");
        const result = t.result;
        if (result !== null && (!result || !nullableText(result.taskId) || typeof result.success !== "boolean"
            || typeof result.confirmationRequired !== "boolean" || !nullableText(result.summary)
            || !nullableText(result.errorMessage) || !nullableText(result.confirmationMessage)
            || !(result.tokenCount === null || typeof result.tokenCount === "number" && Number.isFinite(result.tokenCount)))) {
          throw new Error("Invalid task result");
        }
      }
    }
    return value;
  }

  function renderSnapshot() {
    translated = [];
    byId("project-empty").hidden = Boolean(current);
    byId("project-current").hidden = !current;
    if (!current) return;
    byId("project-current-id").textContent = current.projectId;
    byId("project-current-name").textContent = current.projectName || "";
    byId("metric-requirements").textContent = current.requirements.length;
    byId("metric-pending").textContent = tasks().filter(t => t.status === "PENDING").length;
    byId("metric-completed").textContent = tasks().filter(t => t.status === "SUCCEEDED").length;
    byId("metric-attention").textContent = tasks().filter(t => t.status === "FAILED" || t.status === "NEEDS_CONFIRMATION").length;
    const container = byId("project-requirements");
    container.replaceChildren();
    if (!current.requirements.length) container.append(localized("p", "project.noRequirements"));
    current.requirements.forEach((r, index) => {
      const card = element("article", undefined, "requirement-card");
      card.append(localized("h3", "project.requirementNumber", [index + 1]));
      field(card, "project.original", r.userContent);
      field(card, "project.understanding", r.agentUnderstanding);
      field(card, "project.acceptance", r.acceptanceCriteria);
      if (r.userConfirmMsg) {
        const note = element("div", undefined, "attention-note");
        field(note, "project.confirmation", r.userConfirmMsg);
        card.append(note);
      }
      if (!r.tasks.length) card.append(localized("p", "project.noTasks", [], "form-hint"));
      r.tasks.forEach((t, taskIndex) => {
        const task = element("details", undefined, "task-card");
        task.open = true;
        const heading = element("summary");
        const state = localized("span", statusKeys[t.status], [], "task-status");
        state.dataset.status = t.status;
        heading.append(localized("span", "project.taskNumber", [taskIndex + 1]), state);
        task.append(heading);
        field(task, "project.goal", t.content);
        field(task, "project.acceptance", t.acceptanceCriteria);
        if (t.result) {
          const result = element("div", undefined, "task-result");
          field(result, "project.resultSummary", t.result.summary);
          field(result, "project.resultError", t.result.errorMessage);
          field(result, "project.confirmation", t.result.confirmationMessage);
          field(result, "project.executionId", t.result.taskId);
          if (Number.isSafeInteger(t.result.tokenCount) && t.result.tokenCount >= 0) {
            field(result, "project.tokens", t.result.tokenCount);
          } else {
            const tokens = element("div", undefined, "project-field");
            tokens.append(localized("h4", "project.tokens"), localized("p", "project.unknown"));
            result.append(tokens);
          }
          task.append(result);
        }
        card.append(task);
      });
      container.append(card);
    });
  }

  function renderControls() {
    document.querySelectorAll("[data-project-control]").forEach(control => { control.disabled = busy; });
    const explicit = byId("explicit-models").checked;
    byId("model-fields").hidden = !explicit;
    byId("model-fields").disabled = busy || !explicit;
    byId("project-create").disabled = busy || !goal.value.trim();
    byId("project-open").disabled = busy || !lookup.value.trim();
    byId("project-refresh").disabled = busy || !current;
    byId("project-copy-id").disabled = busy || !current;
    byId("project-add-requirement").disabled = busy || !current || needsRefresh || !requirement.value.trim();
    byId("project-add-prompt").disabled = busy || !current || needsRefresh || !prompt.value.trim();
    byId("project-execute").disabled = busy || !current || needsRefresh || !tasks().some(t => t.status === "PENDING");
    byId("project-stale").hidden = !needsRefresh;
    byId("project-current").setAttribute("aria-busy", String(busy));
  }
  function renderNotice() {
    byId("project-status").textContent = msg(noticeKey);
    byId("project-request-id").textContent = requestId ? msg("project.requestId", requestId) : "";
    byId("project-notice").dataset.state = noticeState;
    renderControls();
  }
  function notice(key, state = "error") {
    noticeKey = key; noticeState = state;
    renderNotice();
  }

  // 项目 ID 在发起时捕获；单飞覆盖创建、查询和写入，语言切换不重建表单或请求。
  async function request({path, method = "GET", body, expectedId, successKey, clearInput}) {
    if (busy) return;
    busy = true;
    requestId = "";
    notice("project.status.pending", "pending");
    const slow = setTimeout(() => notice("project.status.longRunning", "pending"), 15000);
    const write = method === "POST";
    try {
      const response = await fetch(path, {
        method, credentials: "same-origin", headers: {Accept: "application/json", ...(write ? {"Content-Type": "application/json"} : {})},
        ...(write ? {body: JSON.stringify(body)} : {})
      });
      requestId = response.headers.get("X-Request-ID") || "";
      if (!response.ok) {
        if (write && expectedId && ![400, 404].includes(response.status)) {
          needsRefresh = true;
          notice("project.error.uncertain");
        } else {
          if (current && expectedId === current.projectId && response.status === 404) needsRefresh = true;
          const key = {400: "project.error.400", 404: "project.error.404", 503: "project.error.503"}[response.status];
          notice(write && !expectedId && response.status >= 500 && response.status !== 503
            ? "project.error.createUncertain" : key || "project.error.generic");
        }
        return;
      }
      if (successKey === "project.status.promptAdded") {
        if (response.status !== 204) throw new Error("Invalid instruction response");
      } else {
        const data = snapshot(await response.json(), expectedId);
        const changedProject = current && current.projectId !== data.projectId;
        current = data;
        needsRefresh = false;
        lookup.value = current.projectId;
        if (changedProject) { requirement.value = ""; prompt.value = ""; }
        renderSnapshot();
      }
      if (clearInput) clearInput.value = "";
      notice(successKey === "project.status.executed" && attention() ? "project.status.attention" : successKey,
        successKey === "project.status.executed" && attention() ? "error" : "success");
    } catch (_) {
      if (write && expectedId) {
        needsRefresh = true;
        notice("project.error.uncertain");
      } else {
        if (!write && current && expectedId === current.projectId) needsRefresh = true;
        notice(write ? "project.error.createUncertain" : "project.error.connection");
      }
    } finally {
      clearTimeout(slow);
      busy = false;
      renderNotice();
    }
  }

  const roles = [["planning", "project.rolePlanning"], ["development", "project.roleDevelopment"], ["review", "project.roleReview"]];
  const modelFields = [["brand", "project.modelBrand"], ["name", "project.modelName"], ["ver", "project.modelVersion"]];
  for (const [role, title] of roles) {
    const block = element("div", undefined, "role-model");
    block.append(localized("h3", title, [], undefined, modelTranslated));
    for (const [fieldName, key] of modelFields) {
      const input = element("input");
      input.type = "text"; input.id = role + "-" + fieldName; input.autocomplete = "off";
      input.dataset.projectControl = "";
      const label = localized("label", key, [], undefined, modelTranslated);
      label.htmlFor = input.id;
      block.append(label, input);
    }
    byId("role-model-fields").append(block);
  }
  byId("explicit-models").addEventListener("change", renderControls);
  [goal, lookup, requirement, prompt].forEach(input => input.addEventListener("input", renderControls));
  byId("create-project-form").addEventListener("submit", event => {
    event.preventDefault();
    if (busy) return;
    if (!goal.value.trim()) return notice("project.error.required");
    const body = {content: goal.value, projectName: byId("project-name").value || null};
    if (byId("explicit-models").checked) {
      for (const [role] of roles) {
        const model = {};
        for (const [fieldName] of modelFields) {
          const value = byId(role + "-" + fieldName).value;
          if (!value.trim()) return notice("project.error.models");
          model[fieldName] = value;
        }
        body[role + "Agent"] = model;
      }
    }
    request({path: "/api/projects", method: "POST", body, successKey: "project.status.created", clearInput: goal});
  });
  byId("open-project-form").addEventListener("submit", event => {
    event.preventDefault();
    if (busy) return;
    const id = lookup.value.trim();
    if (!validId(id)) return notice("project.error.id");
    request({path: "/api/projects/" + encodeURIComponent(id), expectedId: id, successKey: "project.status.loaded"});
  });
  byId("project-refresh").addEventListener("click", () => {
    if (busy || !current) return;
    const id = current.projectId;
    request({path: "/api/projects/" + encodeURIComponent(id), expectedId: id, successKey: "project.status.loaded"});
  });
  for (const [formId, input, suffix, key] of [
    ["add-requirement-form", requirement, "requirements", "project.status.requirementAdded"],
    ["add-prompt-form", prompt, "prompts", "project.status.promptAdded"]]) {
    byId(formId).addEventListener("submit", event => {
      event.preventDefault();
      if (busy || !current || needsRefresh) return;
      if (!input.value.trim()) return notice("project.error.required");
      const id = current.projectId;
      request({path: "/api/projects/" + encodeURIComponent(id) + "/" + suffix, method: "POST",
        expectedId: id, body: {content: input.value}, successKey: key, clearInput: input});
    });
  }
  byId("project-execute").addEventListener("click", () => {
    if (busy || !current || needsRefresh || !tasks().some(t => t.status === "PENDING")) return;
    const id = current.projectId;
    request({path: "/api/projects/" + encodeURIComponent(id) + "/tasks/execute", method: "POST",
      expectedId: id, body: {}, successKey: "project.status.executed"});
  });
  byId("project-copy-id").addEventListener("click", async () => {
    if (busy || !current) return;
    try { await navigator.clipboard.writeText(current.projectId); notice("project.status.copied", "success"); }
    catch (_) { notice("project.error.copy"); }
  });
  window.addEventListener("beforeunload", event => {
    if (busy) { event.preventDefault(); event.returnValue = ""; }
  });
  window.ZbMessages.onChange(() => {
    [...modelTranslated, ...translated].forEach(({node, key, args}) => { node.textContent = msg(key, ...args); });
    renderNotice();
  });
  renderNotice();
})();
