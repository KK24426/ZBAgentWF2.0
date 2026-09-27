/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：提供聊天首页，委派共享的安全消息模板渲染。
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
public class HomePageController {
    private final LocalizedPageRenderer renderer;

    /**
     * 在启动装配时为聊天首页创建渲染器并校验消息槽，校验失败会阻止此组件完成构造。
     * @param messages 已加载的消息目录，用于检查模板引用的消息key
     * @param locales 请求语言选择器，实际请求到达后才解析语言
     * @param template 装配方已读取的模板文本，本构造器不读取模板文件
     * @throws IllegalStateException 模板含未知消息key或非法占位符
     */
    public HomePageController(MsgCatalog messages, MsgLocaleResolver locales,
                              @Qualifier("homePageTemplate") String template) {
        renderer = new LocalizedPageRenderer(messages, locales, template);
    }

    /**
     * 渲染当前请求语言的聊天首页；仅读取模板与消息快照，不发起业务或模型调用。
     */
    @GetMapping(value = {"/", "/index.html"}, produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> home(HttpServletRequest request) {
        return renderer.render(request);
    }
}
