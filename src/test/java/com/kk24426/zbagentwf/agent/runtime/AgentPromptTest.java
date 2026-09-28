/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：验证不可变规则组合以及附件拒绝，读取规则不调用模型。
 */
package com.kk24426.zbagentwf.agent.runtime;
import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.registry.ProjectFixtureSupport;
import com.kk24426.zbagentwf.common.agent.model.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** 验证不可变规则组合以及附件拒绝，读取规则不调用模型。 */
class AgentPromptTest {
    @TempDir Path temp;
    /** 项目规则下一次读取生效且保留原文，读规则无模型副作用，附件不丢弃。 */
    @Test void immutableRulesPreserveUserTextAndRejectAttachments() throws Exception {
        assertEquals("原文",new Prompt("原文").getPrompt());
        try(var env=new ProjectFixtureSupport(temp.resolve("projects"),"success","success")) {
            var p=env.single();p.setProjectPrompt(new Prompt("项目规则"));var executor=env.factory.getExecutor(p);
            assertTrue(executor.getAllPrompt().getPrompt().contains("项目规则"));
            p.setProjectPrompt(new Prompt("下次规则"));assertTrue(executor.getAllPrompt().getPrompt().contains("下次规则"));
            assertFalse(Files.exists(env.settings.path(p.getProjectId()).resolve("started")));
            p.setProjectPrompt(new Prompt("文件",new byte[]{1}));assertThrows(IllegalArgumentException.class,executor::getAllPrompt);
        }
    }
}
