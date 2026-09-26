/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义执行与只读规划的 Codex 输出约束。
 */
package com.kk24426.zbagentwf.agent.codex;

/** schema 约束输出形状；状态互斥、确认问题等跨字段规则仍由 Java 结果校验负责。 */
final class CodexSchemas {
    private CodexSchemas() { }

    // 所有字段必须出现；没有摘要或原因时写 null，避免“缺字段”和“无内容”混为一谈。
    static final String EXECUTION = """
            {"type":"object","additionalProperties":false,
             "properties":{"success":{"type":"boolean"},"summary":{"type":["string","null"]},
               "errorMessage":{"type":["string","null"]},"confirmationRequired":{"type":"boolean"},
               "confirmationMessage":{"type":["string","null"]}},
             "required":["success","summary","errorMessage","confirmationRequired","confirmationMessage"]}
            """;

    // 模型只规划内容和验收标准；Task 标识、初始状态及项目归属由 Java 侧建立。
    static final String PLANNING = """
            {"type":"object","additionalProperties":false,"properties":{"requirements":{"type":"array",
             "items":{"type":"object","additionalProperties":false,
              "properties":{"agentUnderstanding":{"type":"string"},"acceptanceCriteria":{"type":"string"},
               "userConfirmMsg":{"type":["string","null"]},"tasks":{"type":"array","items":{
                "type":"object","additionalProperties":false,"properties":{"content":{"type":"string"},
                 "acceptanceCriteria":{"type":"string"}},"required":["content","acceptanceCriteria"]}}},
              "required":["agentUnderstanding","acceptanceCriteria","userConfirmMsg","tasks"]}}},
             "required":["requirements"]}
            """;
}
