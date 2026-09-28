/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：验证审核许可不可转移或重用，并在中断和不支持附件时阻断进程。
 */
package com.kk24426.zbagentwf.agent.codex;
import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.agent.model.Prompt;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** 验证审核许可不可转移或重用，并在中断和不支持附件时阻断进程。 */
class CodexSafetyTest {
    @TempDir Path temp;
    /** 只有签发客户端可消费许可，第二次消费明确失败且不产生额外业务调用。 */
    @Test void approvalCannotBeReusedOrTransferredToAnotherClient() throws Exception {
        var client=CodexFixtureSupport.client("success");var other=CodexFixtureSupport.client("success");
        var permit=client.review(temp,new Prompt("只检查文件"),CodexSchemas.EXECUTION,true,v -> {});
        assertThrows(IllegalStateException.class,() -> other.run(permit,v -> {}));
        assertFalse(Files.exists(temp.resolve("business-count")));
        assertNotNull(client.run(permit,v -> {}));
        assertThrows(IllegalStateException.class,() -> client.run(permit,v -> {}));
        assertEquals(1,Files.readAllLines(temp.resolve("business-count")).size());
    }
    /** 在审核成功后中断调用线程，仍必须阻止实际执行并恢复测试线程中断状态。 */
    @Test void interruptionAfterApprovalPreventsActualCall() throws Exception {
        var client=CodexFixtureSupport.client("success");var permit=client.review(temp,new Prompt("任务"),CodexSchemas.EXECUTION,false,v -> {});
        Thread.currentThread().interrupt();
        try {assertThrows(IllegalStateException.class,() -> client.run(permit,v -> {}));}
        finally {Thread.interrupted();}
        assertFalse(Files.exists(temp.resolve("business-count")));
    }
    /** 构造和读取均防御复制附件；未支持内容必须在创建任何进程前拒绝。 */
    @Test void attachmentsAreCopiedAndExplicitlyRejectedBeforeAnyProcess() {
        byte[] source={1,2};var prompt=new Prompt("图片",source);source[0]=9;prompt.getFile()[1]=8;
        assertArrayEquals(new byte[]{1,2},prompt.getFile());
        assertThrows(IllegalArgumentException.class,() -> CodexFixtureSupport.client("success").run(temp,prompt,CodexSchemas.EXECUTION,false,v -> {}));
        assertFalse(Files.exists(temp.resolve("started")));
    }
    /** 计费用量无法表示时保持未知，不把模型已完成的业务重新判为失败。 */
    @Test void tokenAggregationOverflowRemainsUnknown() {
        assertNull(CodexClient.combinedTokens(Long.MAX_VALUE,1L));
        assertNull(CodexClient.combinedTokens(null,1L));assertEquals(5L,CodexClient.combinedTokens(2L,3L));
    }
}
