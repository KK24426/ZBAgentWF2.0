/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：使日志配置或写入故障可见并影响最终退出码。
 */
package com.kk24426.zbagentwf.common.logging;

import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import java.util.concurrent.atomic.AtomicBoolean;

/** 不转发 Logback 原始状态文本，避免错误路径泄漏文件名或凭据。 */
public class LogFailureMonitor implements StatusListener {
    private static final AtomicBoolean FAILED = new AtomicBoolean();

    /**
     * 在新一轮日志准备前清除故障标志；运行期间不靠此方法掩盖已发生的日志写入失败。
     */
    public static void reset() {
        FAILED.set(false);
    }

    public static boolean hasFailed() {
        return FAILED.get();
    }

    /**
     * 只在首次ERROR级状态将标志置为失败并向stderr输出固定提示；不转发可能含路径或凭据的Logback原文。
     */
    @Override
    public void addStatusEvent(Status status) {
        if (status.getEffectiveLevel() >= Status.ERROR && FAILED.compareAndSet(false, true)) {
            System.err.println("日志系统故障：无法完整记录运行日志，请检查配置、目录权限和磁盘空间。");
        }
    }
}
