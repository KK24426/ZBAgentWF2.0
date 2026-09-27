/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：通过只读 Codex 调用规划需求，在完整校验后生成项目数据对象。
 */
package com.kk24426.zbagentwf.agent.codex;

import com.kk24426.zbagentwf.common.project.model.Requirement;
import com.kk24426.zbagentwf.common.project.model.Project;
import com.kk24426.zbagentwf.common.agent.model.Prompt;
import com.kk24426.zbagentwf.agent.runtime.AbstractAgentExecutor;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import com.kk24426.zbagentwf.common.project.model.RequirementTask;
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
    /**
     * 额度与关闭操作使用的归属身份；Project 重载还要求它是持有规则的执行器。
     */
    private final Object owner;
    /**
     * 仅供 Path 重载使用的一次性规则文本；volatile 使后续规划线程可见初始化结果。
     */
    private volatile String initializedRules;

    /**
     * 建立独立额度作用域的只读规划器；构造不会运行 Codex。
     * 使用 Path 重载前须调用 initializePrompts；本类不提供 close 入口。
     * @param client 已配置可执行文件、模型与超时的进程适配器
     */
    public CodexRequirementPlanner(CodexClient client) {
        this(client, new ExecutionResources(), new Object());
    }

    /**
     * 绑定外部提供的资源和归属身份，不占用额度，也不初始化规则。
     * 工厂让每个规划器与其对应执行器共享归属身份，资源生命周期由装配方管理。
     * @param client 本次规划使用的进程适配器
     * @param resources 与其他执行器共享的并发额度和关闭状态
     * @param owner 按对象身份区分的资源归属；Project 重载要求为 AbstractAgentExecutor
     * @throws NullPointerException 任一依赖为空
     */
    public CodexRequirementPlanner(CodexClient client, ExecutionResources resources, Object owner) {
        this.client = Objects.requireNonNull(client);
        this.resources = Objects.requireNonNull(resources);
        this.owner = Objects.requireNonNull(owner);
    }

    /**
     * 为 Path 重载一次性初始化必要规则，保存文本快照；不读取文件或调用模型。
     * Project 重载从绑定执行器取规则，不使用此处的快照。
     * @param defaults 非空白通用规则
     * @param security 非空白安全规则
     * @throws IllegalStateException 重复初始化
     * @throws AgentConfigurationUnavailableException 必要规则缺失或为空白
     */
    public synchronized void initializePrompts(Prompt defaults, Prompt security) {
        if (initializedRules != null) throw new IllegalStateException("规划规则已经初始化。");
        initializedRules = AbstractAgentExecutor.instructions(defaults, security, null, null, null);
    }

    /**
     * 按项目当前目录和绑定执行器的规则同步进行只读规划。
     * 规则在调用前生成快照；项目提示词的后续追加不会改变本次输入。
     * @param project 提供工作目录及当前项目提示词的项目
     * @param content 本次原始需求；原样保存到每条返回需求中
     * @return 全部解析成功的新需求及 PENDING 任务，尚未追加到项目
     * @throws AgentConfigurationUnavailableException owner 不是执行器或其规则未就绪
     * @throws java.util.concurrent.RejectedExecutionException 共享额度已满或资源正在关闭
     * @throws IllegalStateException 进程、协议或规划内容校验失败
     */
    public List<Requirement> plan(Project project, String content) {
        if (!(owner instanceof AbstractAgentExecutor executor)) throw new AgentConfigurationUnavailableException();
        return planWithRules(project.getWorkingDirectory(), content, executor.instructionsFor(project));
    }

    /**
     * 使用 initializePrompts 保存的规则，在给定目录同步执行独立只读规划。
     * 与 Project 重载不同，本入口没有项目、角色和用户规则来源。
     * @param directory 交给 Codex 的工作目录，调用方负责项目目录归属校验
     * @param content 本次需求原文
     * @return 校验完整的新需求列表；由调用方在成功后统一加入项目
     * @throws AgentConfigurationUnavailableException 尚未初始化规则
     * @throws java.util.concurrent.RejectedExecutionException 资源关闭或并发额度已满
     * @throws IllegalStateException 调用或规划结果校验失败
     */
    public List<Requirement> plan(Path directory, String content) {
        String rules = initializedRules;
        if (rules == null) throw new AgentConfigurationUnavailableException();
        return planWithRules(directory, content, rules);
    }

    /**
     * 以 owner 预留单次额度并附着当前调用线程；退出时归还票据，不等待该线程生命周期结束。
     */
    private List<Requirement> planWithRules(Path directory, String content, String rules) {
        // 同步规划也占用全工厂并发额度；附着调用线程，让关闭操作能中断阻塞中的进程调用。
        // 票据仅覆盖本次调用，结束就归还，不能等待可能长期存活的调用线程退出。
        try (var ticket = resources.reserve(owner)) {
            ticket.attach(Thread.currentThread());
            ticket.checkRunning();
            return planAccepted(directory, content, rules);
        }
    }

    /**
     * 在已占额度的范围内调用只读进程并完整校验需求/任务；任何失败均不发布局部列表，受限诊断附入异常链。
     */
    private List<Requirement> planAccepted(Path directory, String content, String rules) {
        var diagnostics = new StringBuilder();
        try {
            String prompt = rules + """
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
            // 先在局部列表完成全部解析；任何一项无效都整体失败，由上层在成功后统一追加。
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
                // 信息不足时只能留下确认问题；不能同时提供可执行任务让调用方误启动开发。
                if (tasks.isEmpty() && (requirement.getUserConfirmMsg() == null || requirement.getUserConfirmMsg().isBlank())) {
                    throw new IllegalStateException("空任务需求必须说明待确认事项。");
                }
                if (!tasks.isEmpty() && requirement.getUserConfirmMsg() != null && !requirement.getUserConfirmMsg().isBlank()) {
                    throw new IllegalStateException("尚待确认的规划不能同时产生可执行 Task。");
                }
                for (JsonNode taskNode : tasks) {
                    CodexJson.fields(taskNode, "content", "acceptanceCriteria");
                    var task = new RequirementTask();
                    // 规划标识由本机生成，与之后每次执行返回的 taskId 分开；初始状态由 Bean 保持 PENDING。
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
