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

    // 审核独立于业务协议，Java还会严格核验字段和非空原因。
    static final String SECURITY = """
            {"type":"object","additionalProperties":false,"properties":{
              "approved":{"type":"boolean"},"reason":{"type":"string"}},"required":["approved","reason"]}
            """;
    // 第一阶段只定义需求与待确认事项，不直接产生可执行Task。
    static final String PLANNING = """
            {"type":"object","additionalProperties":false,"properties":{"requirements":{"type":"array",
              "items":{"type":"object","additionalProperties":false,"properties":{
                "agentUnderstanding":{"type":"string"},"acceptanceCriteria":{"type":"string"},
                "userConfirmMsg":{"type":["string","null"]}},
                "required":["agentUnderstanding","acceptanceCriteria","userConfirmMsg"]}}},"required":["requirements"]}
            """;
    // 第二阶段仅为已明确的单条需求生成任务，数据库负责分配Long标识。
    static final String TASKS = """
            {"type":"object","additionalProperties":false,"properties":{"tasks":{"type":"array","items":{
              "type":"object","additionalProperties":false,"properties":{"content":{"type":"string"},
              "acceptanceCriteria":{"type":"string"}},"required":["content","acceptanceCriteria"]}}},"required":["tasks"]}
            """;
}
