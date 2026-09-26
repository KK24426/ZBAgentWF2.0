/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：实现 Codex 异步受理、单次完成回调和按执行标识读取诊断。
 */
package com.kk24426.zbagentwf.agent.codex;

import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;
import com.kk24426.zbagentwf.common.logging.SecretRedactor;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.common.project.bean.Project;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecutor;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecCallback;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工厂实例共享四个执行额度；诊断按共享资源的容量和保留时间查询。
 * 直接构造的低层实例拥有独立资源作用域，调用方必须 close。
 */
public final class CodexAgentExec extends AgentExecutor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CodexAgentExec.class);
    private final CodexClient client;
    private final Object lifecycle = new Object();
    private final ExecutionResources resources;
    private final boolean ownsResources;
    private boolean closed;

    public CodexAgentExec(AgentBean agent, CodexClient client) {
        this(agent, client, new ExecutionResources(), true);
    }

    public CodexAgentExec(AgentBean agent, CodexClient client, ExecutionResources resources) {
        this(agent, client, resources, false);
    }

    private CodexAgentExec(AgentBean agent, CodexClient client, ExecutionResources resources, boolean ownsResources) {
        super(Objects.requireNonNull(agent));
        this.client = Objects.requireNonNull(client);
        this.resources = Objects.requireNonNull(resources);
        this.ownsResources = ownsResources;
    }

    @Override
    public String exec(Project project, String content, String memory, AgentExecCallback callback) {
        Path directory;
        try {
            Objects.requireNonNull(callback);
            Objects.requireNonNull(project);
            if (content == null || content.isBlank()) throw new IllegalArgumentException();
            directory = project.getWorkingDirectory().toRealPath();
            if (!Files.isDirectory(directory)) throw new IllegalArgumentException();
        } catch (Exception failure) {
            throw new RejectedExecutionException("提交无效：需要有效项目目录、非空内容及回调。", failure);
        }
        synchronized (lifecycle) {
            if (closed) throw new RejectedExecutionException("执行器已关闭。");
            var ticket = resources.reserve(this);
            try {
                Thread worker = Thread.ofVirtual().name("codex-exec-" + ticket.id()).unstarted(
                        () -> execute(ticket, directory, content, memory, callback));
                ticket.attach(worker);
                worker.start();
            }
            catch (RuntimeException | Error failure) {
                ticket.close();
                throw new RejectedExecutionException("无法受理执行。", failure);
            }
            return ticket.id();
        }
    }

    private void execute(ExecutionResources.Ticket ticket, Path directory, String content, String memory, AgentExecCallback callback) {
        String id = ticket.id();
        String[] diagnostic = {""};
        AgentExecResult result = new AgentExecResult();
        result.setTaskId(id);
        try {
            ticket.checkRunning();
            var input = CodexJson.JSON.createObjectNode().put("content", content).put("memory", memory);
            String prompt = """
                    执行以下 JSON 中 content 指定的任务，memory 仅为先前上下文。
                    遵守项目规则及既有权限；不要自行扩大需求、提交或推送代码。
                    按输出 schema 报告实际结果，不把未完成或工具错误报告为成功。
                    如果需要用户决定，立即停止本次工作，success=false、confirmationRequired=true，
                    并在 confirmationMessage 写清问题。普通失败两者均为 false 并提供 errorMessage。
                    成功时 success=true、confirmationRequired=false。不要在结果中输出凭据。
                    """ + CodexJson.JSON.writeValueAsString(input);
            var response = client.run(directory, prompt, CodexSchemas.EXECUTION, false, value -> diagnostic[0] = value);
            result.setTokenCount(response.tokens());
            var value = response.value();
            CodexJson.fields(value, "success", "summary", "errorMessage", "confirmationRequired", "confirmationMessage");
            boolean success = CodexJson.bool(value, "success");
            boolean confirmation = CodexJson.bool(value, "confirmationRequired");
            String error = CodexJson.text(value, "errorMessage", true);
            String message = CodexJson.text(value, "confirmationMessage", true);
            String summary = CodexJson.text(value, "summary", true);
            if (success && confirmation || confirmation && (message == null || message.isBlank())
                    || !success && !confirmation && (error == null || error.isBlank())) {
                throw new IllegalStateException("Codex 最终结果状态互相矛盾或缺少原因。");
            }
            result.setSuccess(success);
            result.setConfirmationRequired(confirmation);
            result.setErrorMessage(error == null ? null : SecretRedactor.redact(error));
            result.setSummary(summary == null ? null : SecretRedactor.redact(summary));
            result.setConfirmationMessage(message == null ? null : SecretRedactor.redact(message));
        } catch (Throwable failure) {
            // 不将模型自由文本或进程异常消息复制到业务错误中，完整异常链由日志编码器隐藏消息。
            result.setSuccess(false);
            result.setConfirmationRequired(false);
            result.setErrorMessage("Codex 执行未正常完成，请按执行标识检查诊断。");
            LOG.warn("Codex 执行失败", failure);
        } finally {
            // 进程已经收尾；先归还额度再回调，允许回调重入工厂关闭。
            ticket.complete(diagnostic[0]);
            LOG.debug("Codex 执行结束 taskId={} success={} confirmationRequired={}",
                    id, result.isSuccess(), result.isConfirmationRequired());
            try { callback.onCompleted(result); }
            catch (Throwable failure) {
                // 调用方回调异常不能被解释成第二次执行失败，也不能再次调用回调。
                LOG.warn("Codex 完成回调失败", failure);
            }
        }
    }

    @Override
    public String getStderr(String taskId) {
        return resources.diagnostic(this, taskId);
    }

    /** 停止受理并中断正在运行的进程调用；清空诊断，允许从完成回调重入关闭。 */
    @Override
    public void close() {
        synchronized (lifecycle) {
            if (closed) return;
            closed = true;
        }
        if (ownsResources) resources.close();
        else resources.closeOwner(this);
    }
}
