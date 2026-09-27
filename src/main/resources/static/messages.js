/* 两个页面共享语言选择；只保存语言 Cookie，不持久化业务输入或结果。 */
(() => {
  "use strict";
  const catalog = JSON.parse(document.getElementById("msg-catalog").content.textContent);
  const select = document.getElementById("language-select");
  const listeners = [];
  let language = catalog.language;

  function msg(key, ...args) {
    const text = catalog.messages[language][key];
    if (typeof text !== "string") throw new Error("Missing message key");
    return text.replace(/\{(0|[1-9][0-9]*)}/g, (_, index) => {
      if (args[Number(index)] === undefined) throw new Error("Missing message argument");
      return String(args[Number(index)]);
    });
  }

  function renderLanguage() {
    document.documentElement.lang = language;
    document.querySelectorAll("[data-msg]").forEach(element => {
      element.textContent = msg(element.dataset.msg);
    });
    for (const attribute of ["aria-label", "placeholder", "content"]) {
      const marker = "data-msg-" + attribute;
      document.querySelectorAll("[" + marker + "]").forEach(element => {
        element.setAttribute(attribute, msg(element.getAttribute(marker)));
      });
    }
    listeners.forEach(listener => listener());
  }

  Object.defineProperty(window, "ZbMessages", {value: Object.freeze({
    msg, onChange(listener) { listeners.push(listener); }
  })});
  select.value = catalog.preference;
  select.disabled = false;
  select.addEventListener("change", () => {
    const selected = select.value;
    if (selected !== "auto" && !Object.hasOwn(catalog.messages, selected)) return;
    const secure = location.protocol === "https:" ? "; Secure" : "";
    document.cookie = "zb.locale=" + (selected === "auto" ? "" : encodeURIComponent(selected))
      + "; Max-Age=" + (selected === "auto" ? "0" : "31536000") + "; Path=/; SameSite=Lax" + secure;
    language = selected === "auto" ? catalog.automatic : selected;
    renderLanguage();
  });
  renderLanguage();
})();
