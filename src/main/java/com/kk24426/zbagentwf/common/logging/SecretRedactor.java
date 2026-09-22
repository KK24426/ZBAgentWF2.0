/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：在完整日志文本上替换已知凭据和常见敏感字段。
 */
package com.kk24426.zbagentwf.common.logging;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** 脱敏是已知字段的防护，不替代调用方避免记录原始业务数据的责任。 */
public final class SecretRedactor {
    private static final Set<String> KNOWN = ConcurrentHashMap.newKeySet();
    private static final Pattern FIELDS = Pattern.compile(
            "(?i)([\\\"']?(?:password|passwd|pwd|token|api[-_]?key|secret|username|user|authorization)[\\\"']?\\s*[:=]\\s*)"
            + "(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;}&]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/-]+=*");
    private static final Pattern JDBC = Pattern.compile("jdbc:mysql:[^\\s\\\"']+");

    private SecretRedactor() {
    }

    public static void register(String value) {
        if (value != null && !value.isBlank()) {
            KNOWN.add(value);
        }
    }

    public static String redact(String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        for (String secret : KNOWN.stream().sorted((a, b) -> Integer.compare(b.length(), a.length())).toList()) {
            result = result.replace(secret, "[REDACTED]");
        }
        result = BEARER.matcher(result).replaceAll("Bearer [REDACTED]");
        result = FIELDS.matcher(result).replaceAll("$1[REDACTED]");
        return JDBC.matcher(result).replaceAll("jdbc:mysql:[REDACTED]");
    }
}
