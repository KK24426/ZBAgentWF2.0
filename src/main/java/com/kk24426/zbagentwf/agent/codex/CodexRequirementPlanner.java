/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：分别规划需求与单条需求任务，所有最终输入经过只读安全审核。
 */
package com.kk24426.zbagentwf.agent.codex;

import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.agent.model.Prompt;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;

/** 只返回完整校验后的暂存数据；数据库ID与项目发布由上层统一处理。 */
public final class CodexRequirementPlanner {
    private final CodexClient client;
    private final ExecutionResources resources;
    private final Object owner;
    /** 绑定工厂共享资源，不自行拥有关闭责任，不执行模型。 */
    public CodexRequirementPlanner(CodexClient client, ExecutionResources resources, Object owner) {
        this.client=Objects.requireNonNull(client); this.resources=Objects.requireNonNull(resources); this.owner=Objects.requireNonNull(owner);
    }
    /** 第一阶段只生成需求；待确认需求由调用方保留为空任务，不猜测缺失业务信息。 */
    public List<Requirement> plan(Path directory, String content, String rules) {
        return accepted(() -> {
            Prompt prompt = new Prompt(rules + "\n" + """
                    仅只读分析需求，禁止实施和修改文件。本次是需求规划阶段，仅按本次schema返回requirements。
                    每项包含理解、验收标准和待确认问题；信息充分时userConfirmMsg为null，不在此阶段拆分Task。
                    用户输入（JSON数据）：
                    """ + CodexJson.JSON.writeValueAsString(content));
            var response = call(directory,prompt,CodexSchemas.PLANNING);
            CodexJson.fields(response,"requirements");
            JsonNode nodes=response.get("requirements");
            if (!nodes.isArray() || nodes.isEmpty()) throw new IllegalStateException("规划未产生需求。");
            var result=new ArrayList<Requirement>();
            for (JsonNode node:nodes) {
                CodexJson.fields(node,"agentUnderstanding","acceptanceCriteria","userConfirmMsg");
                var r=new Requirement(); r.setUserContent(content);
                r.setAgentUnderstanding(CodexJson.text(node,"agentUnderstanding",false));
                r.setAcceptanceCriteria(CodexJson.text(node,"acceptanceCriteria",false));
                r.setUserConfirmMsg(CodexJson.text(node,"userConfirmMsg",true)); result.add(r);
            }
            return result;
        });
    }
    /** 第二阶段仅拆分一条已明确需求；已有任务或待确认问题不能通过重新规划静默覆盖。 */
    public List<RequirementTask> tasks(Path directory, Requirement requirement, String rules) {
        if (requirement.getTasks()==null || !requirement.getTasks().isEmpty()) throw new IllegalArgumentException("需求已有任务，不能覆盖。");
        if (requirement.getUserConfirmMsg()!=null && !requirement.getUserConfirmMsg().isBlank())
            throw new IllegalArgumentException("需求仍需用户确认。");
        return accepted(() -> {
            var input=CodexJson.JSON.createObjectNode().put("userContent",requirement.getUserContent())
                .put("agentUnderstanding",requirement.getAgentUnderstanding()).put("acceptanceCriteria",requirement.getAcceptanceCriteria());
            Prompt prompt=new Prompt(rules + "\n仅只读拆分以下单条需求，禁止执行或增加需求；按本次schema返回有序tasks。输入JSON：\n" + CodexJson.JSON.writeValueAsString(input));
            var response=call(directory,prompt,CodexSchemas.TASKS); CodexJson.fields(response,"tasks");
            var nodes=response.get("tasks");
            if (!nodes.isArray() || nodes.isEmpty()) throw new IllegalStateException("任务拆分结果不能为空。");
            var result=new ArrayList<RequirementTask>();
            for (JsonNode node:nodes) {
                CodexJson.fields(node,"content","acceptanceCriteria"); var task=new RequirementTask();
                task.setContent(CodexJson.text(node,"content",false)); task.setAcceptanceCriteria(CodexJson.text(node,"acceptanceCriteria",false)); result.add(task);
            }
            return result;
        });
    }
    /** 每个同步调用只占一个共享名额，附着当前线程以支持工厂关闭中断。 */
    private <T> T accepted(Supplier<T> work) {
        try (var ticket=resources.reserve(owner)) {
            ticket.attach(Thread.currentThread()); ticket.checkRunning(); return work.get();
        }
    }
    /** 审核和业务规划都只读；诊断限长脱敏后保留异常链，不发布不完整业务数据。 */
    private JsonNode call(Path directory, Prompt prompt, String schema) {
        var diagnostics=new StringBuilder();
        try { return client.run(directory,prompt,schema,true,value -> append(diagnostics,value)).value(); }
        catch (RuntimeException failure) {
            if (!diagnostics.isEmpty()) failure.addSuppressed(new IllegalStateException(diagnostics.toString()));
            throw new IllegalStateException("需求或任务规划失败，未发布本次数据。",failure);
        }
    }
    /** 聚合诊断的字符预算不超过64KiB字节预算，按UTF-8编码逐码点截断。 */
    static void append(StringBuilder target,String value) {
        if (value==null || value.isEmpty()) return;
        int used=target.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        for (int i=0;i<value.length();) {
            int cp=value.codePointAt(i); String part=new String(Character.toChars(cp));
            int bytes=part.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (used+bytes>65000) { if (!target.toString().endsWith("[诊断已截断]")) target.append("[诊断已截断]"); break; }
            target.append(part); used+=bytes; i+=Character.charCount(cp);
        }
    }
}
