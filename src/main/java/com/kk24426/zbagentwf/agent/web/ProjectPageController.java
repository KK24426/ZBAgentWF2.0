/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：提供项目操作页面，不新增项目业务API。
 */
package com.kk24426.zbagentwf.agent.web;

import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 固定页面入口；模板和消息数据均通过共享渲染器转义。 */
@RestController
public class ProjectPageController {
    private final LocalizedPageRenderer renderer;

    public ProjectPageController(MsgCatalog messages, MsgLocaleResolver locales,
                              @Qualifier("projectPageTemplate") String template) {
        renderer = new LocalizedPageRenderer(messages, locales, template);
    }

    @GetMapping(value = {"/projects", "/projects.html"}, produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> projects(HttpServletRequest request) {
        return renderer.render(request);
    }
}
