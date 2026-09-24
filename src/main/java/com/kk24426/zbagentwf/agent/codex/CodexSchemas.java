/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义执行与只读规划的 Codex 输出约束。
 */
package com.kk24426.zbagentwf.agent.codex;

final class CodexSchemas {
    private CodexSchemas() { }

    static final String EXECUTION = """
            {"type":"object","additionalProperties":false,
             "properties":{"success":{"type":"boolean"},"summary":{"type":["string","null"]},
               "errorMessage":{"type":["string","null"]},"confirmationRequired":{"type":"boolean"},
               "confirmationMessage":{"type":["string","null"]}},
             "required":["success","summary","errorMessage","confirmationRequired","confirmationMessage"]}
            """;

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
