/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：通过只读 Codex 调用规划需求，在完整校验后生成项目数据对象。
 */
package com.kk24426.zbagentwf.agent.codex;

import com.kk24426.zbagentwf.common.project.bean.Requirement;
import com.kk24426.zbagentwf.common.project.bean.RequirementTask;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/** 项目实现使用的具体规划辅助类，不注册新用户业务接口。 */
public final class CodexRequirementPlanner {
    private final CodexClient client;
    private final ExecutionResources resources;
    private final Object owner;

    public CodexRequirementPlanner(CodexClient client) {
        this(client, new ExecutionResources(), new Object());
    }

    public CodexRequirementPlanner(CodexClient client, ExecutionResources resources, Object owner) {
        this.client = Objects.requireNonNull(client);
        this.resources = Objects.requireNonNull(resources);
        this.owner = Objects.requireNonNull(owner);
    }

    /** 仅生成尚未执行的数据；调用方在本方法全部成功后才加入项目。 */
    public List<Requirement> plan(Path directory, String content) {
        try (var ticket = resources.reserve(owner)) {
            ticket.attach(Thread.currentThread());
            ticket.checkRunning();
            return planAccepted(directory, content);
        }
    }

    private List<Requirement> planAccepted(Path directory, String content) {
        var diagnostics = new StringBuilder();
        try {
            String prompt = """
                    仅分析用户需求和当前项目，禁止修改文件、执行实现任务、提交或推送。
                    将用户输入规划为需求列表，每项给出理解、可观察的验收标准和按执行顺序排列的 Task。
                    不添加用户未要求的功能或业务规则；信息不足时在 userConfirmMsg 写待确认问题，
                    该需求 tasks 为空，不臆造实现。信息足够时 userConfirmMsg 为 null。
                    Task 只含 content 和 acceptanceCriteria。严格遵循输出 schema，不输出凭据。
                    用户输入（JSON 字符串）：
                    """ + CodexJson.JSON.writeValueAsString(content);
            var response = client.run(directory, prompt, CodexSchemas.PLANNING, true, diagnostics::append);
            CodexJson.fields(response.value(), "requirements");
            JsonNode nodes = response.value().get("requirements");
            if (!nodes.isArray() || nodes.isEmpty()) throw new IllegalStateException("规划未产生需求。");
            var result = new ArrayList<Requirement>();
            for (JsonNode node : nodes) {
                CodexJson.fields(node, "agentUnderstanding", "acceptanceCriteria", "userConfirmMsg", "tasks");
                var requirement = new Requirement();
                requirement.setUserContent(content);
                requirement.setAgentUnderstanding(CodexJson.text(node, "agentUnderstanding", false));
                requirement.setAcceptanceCriteria(CodexJson.text(node, "acceptanceCriteria", false));
                requirement.setUserConfirmMsg(CodexJson.text(node, "userConfirmMsg", true));
                JsonNode tasks = node.get("tasks");
                if (!tasks.isArray()) throw new IllegalStateException("规划 Task 列表无效。");
                if (tasks.isEmpty() && (requirement.getUserConfirmMsg() == null || requirement.getUserConfirmMsg().isBlank())) {
                    throw new IllegalStateException("空任务需求必须说明待确认事项。");
                }
                if (!tasks.isEmpty() && requirement.getUserConfirmMsg() != null && !requirement.getUserConfirmMsg().isBlank()) {
                    throw new IllegalStateException("尚待确认的规划不能同时产生可执行 Task。");
                }
                for (JsonNode taskNode : tasks) {
                    CodexJson.fields(taskNode, "content", "acceptanceCriteria");
                    var task = new RequirementTask();
                    task.setId(UUID.randomUUID().toString());
                    task.setContent(CodexJson.text(taskNode, "content", false));
                    task.setAcceptanceCriteria(CodexJson.text(taskNode, "acceptanceCriteria", false));
                    requirement.getTasks().add(task);
                }
                result.add(requirement);
            }
            return result;
        } catch (RuntimeException failure) {
            if (!diagnostics.isEmpty()) {
                // 保留受限脱敏诊断在异常链中供本地排查；日志编码器继续隐藏全部自由文本。
                failure.addSuppressed(new IllegalStateException("Codex 规划诊断：\n" + diagnostics));
            }
            LoggerFactory.getLogger(CodexRequirementPlanner.class).warn("Codex 需求规划失败", failure);
            throw new IllegalStateException("需求规划失败，原需求列表未修改。", failure);
        }
    }
}
