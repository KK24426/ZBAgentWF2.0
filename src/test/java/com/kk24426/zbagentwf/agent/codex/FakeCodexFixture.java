/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：以真实 Java 子进程提供可控 Codex JSONL 测试协议，不调用模型。
 */
package com.kk24426.zbagentwf.agent.codex;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** 只进入测试类路径；所有落盘数据均为测试生成的临时数据。 */
public class FakeCodexFixture {
    /** 按安全/需求/任务/执行schema返回可控事件；落盘标记仅用于协议测试，并非真正OS只读沙箱。 */
    public static void main(String[] args) throws Exception {
        String scenario = args[0];
        if (scenario.equals("child")) {
            Files.writeString(Path.of("child-ready"), "ready");
            new java.util.concurrent.CountDownLatch(1).await();
            return;
        }
        var arguments = List.of(args);
        Files.writeString(Path.of("model-selection"), arguments.get(arguments.indexOf("--model") + 1));
        Path schema = Path.of(arguments.get(arguments.indexOf("--output-schema") + 1));
        Files.writeString(Path.of("started"), Long.toString(ProcessHandle.current().pid()));
        Files.writeString(Path.of("schema-path"), schema.toString());
        String schemaText=Files.readString(schema);
        if (schemaText.contains("\"approved\"")) {
            if (!arguments.get(arguments.indexOf("--sandbox")+1).equals("read-only")) throw new AssertionError("audit sandbox");
            String audit=new String(System.in.readAllBytes(),StandardCharsets.UTF_8);
            Files.writeString(Path.of("audit-input.txt"),audit);
            Files.writeString(Path.of("audit-count"),"1\n",StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
            if (scenario.equals("audit-sleep")) new java.util.concurrent.CountDownLatch(1).await();
            if (scenario.equals("audit-bad")) { System.out.println("not-json"); return; }
            if (scenario.equals("audit-exit")) System.exit(7);
            var verdict=CodexJson.JSON.createObjectNode().put("approved",!scenario.equals("audit-deny")).put("reason","fixture review");
            if (scenario.equals("audit-field")) verdict.put("extra",true);
            if (scenario.equals("audit-empty")) verdict.put("reason","");
            emit(verdict); return;
        }
        Files.writeString(Path.of("business-count"),"1\n",StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
        if (scenario.equals("descendant")) {
            String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
            Process child = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"),
                    FakeCodexFixture.class.getName(), "child").inheritIO().start();
            Files.writeString(Path.of("child-pid"), Long.toString(child.pid()));
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (!Files.exists(Path.of("child-ready")) && System.nanoTime() < deadline) Thread.sleep(10);
            if (!Files.exists(Path.of("child-ready"))) throw new IllegalStateException("child not ready");
        }
        if (scenario.equals("sleep") || scenario.equals("no-input")) {
            new java.util.concurrent.CountDownLatch(1).await();
        }
        if (scenario.equals("flood")) System.err.print("password=fixture-secret\n" + "诊断".repeat(70000));
        else System.err.print("fixture diagnostic token=fixture-secret\n");
        String prompt = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        Files.writeString(Path.of("prompt-input.txt"), prompt, StandardCharsets.UTF_8);
        if (scenario.equals("auth-failure")) {
            System.err.println("fixture authentication failed token=fixture-secret");
            System.exit(7);
        }
        if (scenario.equals("bad-json")) { System.out.println("not json"); return; }
        if (scenario.equals("output-limit")) { System.out.print("x".repeat(9 * 1024 * 1024)); return; }
        var response = CodexJson.JSON.createObjectNode();
        if (Files.readString(schema).contains("agentUnderstanding")) {
            if (!arguments.get(arguments.indexOf("--sandbox") + 1).equals("read-only")) {
                throw new IllegalStateException("planning must be read-only");
            }
            for (int r = 0; r < 2; r++) {
                var requirement = response.withArray("requirements").addObject();
                requirement.put("agentUnderstanding", "理解" + r).put("acceptanceCriteria", "需求验收" + r)
                        .putNull("userConfirmMsg");
            }
            if (scenario.equals("bad-plan")) {
                ((tools.jackson.databind.node.ObjectNode) response.get("requirements").get(1)).put("agentUnderstanding", "");
            }
        } else if (schemaText.contains("\"tasks\"")) {
            for (int t=0;t<2;t++) response.withArray("tasks").addObject().put("content","执行任务"+t).put("acceptanceCriteria","Task验收");
            if (scenario.equals("bad-tasks")) response.put("tasks","invalid");
        } else {
            boolean confirmation = scenario.equals("confirmation");
            boolean success = !confirmation && !scenario.equals("failure");
            response.put("success", success).put("confirmationRequired", confirmation)
                    .put("summary", "cwd=" + Path.of("").toRealPath() + "\nargs=" + arguments + "\n" + prompt);
            if (confirmation) response.put("confirmationMessage", "请确认执行范围"); else response.putNull("confirmationMessage");
            if (success || confirmation) response.putNull("errorMessage"); else response.put("errorMessage", "普通失败");
            if (scenario.equals("contradiction")) response.put("success", true).put("confirmationRequired", true);
        }
        System.out.println("{\"type\":\"thread.started\",\"thread_id\":\"fixture\"}");
        var message = CodexJson.JSON.createObjectNode().put("type", "item.completed");
        message.putObject("item").put("type", "agent_message").put("text", CodexJson.JSON.writeValueAsString(response));
        System.out.println(CodexJson.JSON.writeValueAsString(message));
        if (scenario.equals("missing-completion")) return;
        if (scenario.equals("event-error")) System.out.println("{\"type\":\"error\",\"message\":\"private fixture text\"}");
        String end = scenario.equals("unknown-tokens") ? "{\"type\":\"turn.completed\"}"
                : "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":11,\"cached_input_tokens\":8,\"output_tokens\":7}}";
        System.out.println(end);
        if (scenario.equals("duplicate-completion")) System.out.println(end);
        if (scenario.equals("exit-failure")) System.exit(7);
    }
    /** 输出与真实CLI一致的终态事件；仅用于本地fixture。 */
    private static void emit(tools.jackson.databind.JsonNode response) {
        var event=CodexJson.JSON.createObjectNode().put("type","item.completed");
        event.putObject("item").put("type","agent_message").put("text",CodexJson.JSON.writeValueAsString(response));
        System.out.println(CodexJson.JSON.writeValueAsString(event));
        System.out.println("{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":11,\"output_tokens\":7}}");
    }

}
