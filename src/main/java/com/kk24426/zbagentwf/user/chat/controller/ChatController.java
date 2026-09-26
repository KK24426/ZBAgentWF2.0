/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.3
 * 功能概要：接收单次聊天请求、调用用户服务并返回安全的结果或固定错误。
 */
package com.kk24426.zbagentwf.user.chat.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kk24426.zbagentwf.common.chat.ChatRequest;
import com.kk24426.zbagentwf.common.chat.ChatResponse;
import com.kk24426.zbagentwf.common.exception.AgentUnavailableException;
import com.kk24426.zbagentwf.common.msg.MsgCatalog;
import com.kk24426.zbagentwf.user.chat.service.ChatService;
import java.util.Locale;

/** 只开放已批准的 POST /api/chat，不扩展为通用 Agent 调用 API。 */
@RestController
public class ChatController {
    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final MsgCatalog messages;

    public ChatController(ChatService chatService, MsgCatalog messages) {
        this.chatService = chatService;
        this.messages = messages;
    }

    @PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reply(@RequestBody ChatRequest request, Locale locale) {
        // 先区分输入错误，避免将 Agent 内部的 IllegalArgumentException 误报为用户错误。
        if (request == null || request.message() == null || request.message().isBlank()
                || request.message().length() > 4000) {
            return invalidRequest(locale);
        }
        try {
            return ResponseEntity.ok(new ChatResponse(chatService.reply(request.message())));
        } catch (AgentUnavailableException failure) {
            LOG.warn("聊天调用不可用", failure);
            return error(503, "http.chat.unavailable", locale);
        } catch (RuntimeException failure) {
            // 记录完整异常交由 SanitizingEncoder 隐藏自由文本，响应不包含任何原始输入。
            LOG.error("聊天调用失败", failure);
            return error(500, "http.internalError", locale);
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> malformedRequest(HttpMessageNotReadableException failure, Locale locale) {
        LOG.warn("聊天请求正文无效", failure);
        return invalidRequest(locale);
    }

    private ResponseEntity<String> invalidRequest(Locale locale) {
        return error(400, "http.chat.invalid", locale);
    }

    private ResponseEntity<String> error(int status, String key, Locale locale) {
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .header("Content-Language", MsgCatalog.languageTag(locale)).body(messages.get(key, locale));
    }
}
