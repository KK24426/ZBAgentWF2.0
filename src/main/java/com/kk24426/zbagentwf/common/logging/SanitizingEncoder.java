/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：对消息和异常完整渲染结果统一脱敏后编码。
 */
package com.kk24426.zbagentwf.common.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

/** 避免只处理 %msg 而遗漏 %ex 中的凭据；保留异常类型和调用链。 */
public class SanitizingEncoder extends PatternLayoutEncoder {
    @Override
    public byte[] encode(ILoggingEvent event) {
        return SecretRedactor.redact(getLayout().doLayout(event)).getBytes(getCharset());
    }
}
