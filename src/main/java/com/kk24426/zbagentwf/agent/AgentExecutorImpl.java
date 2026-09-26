/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：实现 Codex 异步受理、单次完成回调和按执行标识读取诊断。
 */
package com.kk24426.zbagentwf.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import com.kk24426.zbagentwf.common.agent.bean.AgentExecResult;
import com.kk24426.zbagentwf.common.agent.bean.Prompt;
import com.kk24426.zbagentwf.common.logging.SecretRedactor;
import com.kk24426.zbagentwf.common.project.bean.Project;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecCallback;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecutor;

/**
 * 执行器父类,所有模型通用的方法应该写在这里面,不同的写在子类立马,如CodexAgentExec
 */
public class AgentExecutorImpl extends AgentExecutor {

}
