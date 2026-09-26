/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：校验 Codex 最终结构化回复，不接受缺字段或类型转换。
 */
package com.kk24426.zbagentwf.agent.codex;

import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 仅用于此适配器的协议解析。 */
final class CodexJson {
    // 重复字段和尾随 JSON 都可能改变解释结果，解析阶段即拒绝，不做宽松纠错。
    static final JsonMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    private CodexJson() { }

    /** 字段集合必须完全一致；额外字段与缺字段都说明返回值偏离当前协议。 */
    static void fields(JsonNode node, String... names) {
        if (node == null || !node.isObject() || !Set.copyOf(node.propertyNames()).equals(Set.of(names))) {
            throw new IllegalStateException("Codex 回复字段不符合约定。");
        }
    }

    static String text(JsonNode node, String name, boolean nullable) {
        JsonNode value = node.get(name);
        // nullable 只允许显式 JSON null，不代表字段可以缺失；必填文本还必须非空白。
        if (nullable && value != null && value.isNull()) return null;
        if (value == null || !value.isString() || (!nullable && value.asString().isBlank())) {
            throw new IllegalStateException("Codex 回复文本不符合约定。");
        }
        return value.asString();
    }

    /** 状态只接受 JSON 布尔值，避免把字符串或数值转换为成功标志。 */
    static boolean bool(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isBoolean()) throw new IllegalStateException("Codex 回复状态无效。");
        return value.booleanValue();
    }
}
