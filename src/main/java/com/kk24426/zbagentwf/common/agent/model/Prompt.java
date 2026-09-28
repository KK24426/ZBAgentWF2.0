/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：保存不可变提示词及附件快照，构造不调用模型；安全审核由执行入口显式完成。
 */
package com.kk24426.zbagentwf.common.agent.model;

/** 保存不可变提示词及附件快照，构造不调用模型；安全审核由执行入口显式完成。 */
public final class Prompt {
    private final String prompt;
    /** 附件原始内容；null表示无附件，当前协议不支持非空附件。 */
    private final byte[] file;
    /** 保存纯文本；可选规则允许null，必需约束由使用入口检查。 */
    public Prompt(String prompt) { this(prompt, null); }
    /** 复制附件，避免调用方在审核后改变输入。 */
    public Prompt(String prompt, byte[] file) {
        this.prompt = prompt;
        this.file = file == null ? null : file.clone();
    }
    public String getPrompt() { return prompt; }
    /** 返回防御副本，不能通过返回数组修改已受理快照。 */
    public byte[] getFile() { return file == null ? null : file.clone(); }
    /** 当前文本协议不能静默忽略附件；以后支持时须同步扩展审核与传输。 */
    public void requireTextOnly() {
        if (file != null && file.length != 0) throw new IllegalArgumentException("当前模型调用尚不支持附件。");
    }
}
