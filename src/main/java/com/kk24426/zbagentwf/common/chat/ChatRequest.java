/*
 * 创建日期：2026-09-24
 * 更新日期：2026-09-24
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：承载单次聊天消息，拒绝 JSON 非字符串值的隐式转换。
 */
package com.kk24426.zbagentwf.common.chat;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/** HTTP 请求正文；缺失、null、空白和长度由请求入口及业务服务校验。 */
public record ChatRequest(@JsonDeserialize(using = MessageDeserializer.class) String message) {
    /** 仅约束本字段，不改变全局 Jackson 的转换契约。 */
    public static final class MessageDeserializer extends ValueDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return context.reportInputMismatch(String.class, "消息必须是字符串。");
            }
            return parser.getString();
        }
    }
}
