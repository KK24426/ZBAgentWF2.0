/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：显式加载并校验三语消息，提供不可变的消息查询与纯文本参数替换。
 */
package com.kk24426.zbagentwf.common.msg;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 消息目录是初始化后的只读快照；不在查询时访问文件，不使用 JVM 默认语言。 */
public final class MsgCatalog {
    public static final List<String> LANGUAGES = List.of("zh-CN", "en", "ja");
    private static final Map<String, String> FILES = Map.of(
            "zh-CN", "msg_zh_CN.properties", "en", "msg_en.properties", "ja", "msg_ja.properties");
    private static final Pattern ARGUMENT = Pattern.compile("\\{(0|[1-9][0-9]*)}");
    // Java/JS 的动态分支不能通过首页占位符发现，在启动时单独检查这些必需消息。
    private static final Set<String> REQUIRED = Set.of(
            "chat.send", "chat.sending", "chat.status.waiting", "chat.status.pending", "chat.status.failed",
            "chat.status.success", "chat.initial", "chat.pending", "chat.requestId", "chat.validation.required",
            "chat.validation.tooLong", "chat.error.400", "chat.error.405", "chat.error.415", "chat.error.503",
            "chat.error.generic", "chat.error.timeout", "chat.error.connection",
            "http.unavailable", "http.methodNotAllowed", "http.notFound", "http.jsonRequired",
            "http.internalError", "http.chat.invalid", "http.chat.unavailable",
            "http.project.invalid", "http.project.notFound", "http.project.unavailable",
            "project.requirementNumber", "project.taskNumber", "project.original",
            "project.understanding", "project.acceptance", "project.confirmation",
            "project.noTasks", "project.noRequirements", "project.resultSummary",
            "project.resultError", "project.executionId", "project.tokens",
            "project.unknown", "project.task.pending", "project.task.running",
            "project.task.succeeded", "project.task.failed", "project.task.confirmation",
            "project.status.ready", "project.status.pending", "project.status.longRunning",
            "project.status.created", "project.status.loaded", "project.status.requirementAdded",
            "project.status.promptAdded", "project.status.executed", "project.status.attention",
            "project.status.copied", "project.error.copy", "project.error.required",
            "project.error.id", "project.error.models", "project.error.400",
            "project.error.404", "project.error.503", "project.error.generic",
            "project.error.connection", "project.error.uncertain", "project.error.createUncertain",
            "project.requestId");

    private final Map<String, Map<String, String>> messages;

    /** 保存语言索引的不可变副本；各语言消息表由load预先复制，后续查询不重读外部文件。 */
    private MsgCatalog(Map<String, Map<String, String>> messages) {
        this.messages = Map.copyOf(messages);
    }

    /** 启动装配显式调用；外部目录相对启动工作目录，缺文件合法，存在但不可读则失败。 */
    public static MsgCatalog load(Path externalDirectory) throws IOException {
        if (!Files.notExists(externalDirectory, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(externalDirectory)) {
            throw new IOException("msg 外部配置目录不可用。");
        }
        var builtIn = new LinkedHashMap<String, Map<String, String>>();
        for (String language : LANGUAGES) {
            String filename = FILES.get(language);
            try (InputStream stream = MsgCatalog.class.getResourceAsStream("/msg/" + filename)) {
                if (stream == null) throw new IllegalStateException("缺少内置 msg 语言文件。");
                builtIn.put(language, read(stream));
            }
        }
        Map<String, String> chinese = builtIn.get("zh-CN");
        if (!chinese.keySet().containsAll(REQUIRED)) {
            throw new IllegalStateException("内置 msg 缺少必需消息。");
        }
        for (var localized : builtIn.values()) {
            if (!chinese.keySet().equals(localized.keySet())) {
                throw new IllegalStateException("内置 msg 三语 key 不一致。");
            }
            for (String key : chinese.keySet()) validateTranslation(chinese.get(key), localized.get(key));
        }

        var merged = new LinkedHashMap<String, Map<String, String>>();
        for (String language : LANGUAGES) {
            var values = new LinkedHashMap<>(builtIn.get(language));
            Path file = externalDirectory.resolve(FILES.get(language));
            // notExists=false 也覆盖无权检查、悬空链接等情形，随后实际读取会明确失败。
            if (!Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) {
                try (InputStream stream = Files.newInputStream(file)) {
                    Map<String, String> overrides = read(stream);
                    for (var entry : overrides.entrySet()) {
                        if (!values.containsKey(entry.getKey())) {
                            throw new IllegalStateException("外部 msg 包含未知 key。");
                        }
                        validateTranslation(values.get(entry.getKey()), entry.getValue());
                        values.put(entry.getKey(), entry.getValue());
                    }
                } catch (IOException failure) {
                    throw new IOException("外部 msg 文件读取失败。", failure);
                }
            }
            merged.put(language, Map.copyOf(values));
        }
        return new MsgCatalog(merged);
    }

    /** 获取消息；仅替换 {0} 等文本参数，不解释 HTML、数字格式或单引号。 */
    public String get(String key, Locale locale, String... args) {
        String template = messages.get(languageTag(locale)).get(key);
        if (template == null) throw new IllegalArgumentException("消息 key 不存在。");
        Matcher matcher = ARGUMENT.matcher(template);
        var result = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index >= args.length || args[index] == null) {
                throw new IllegalArgumentException("消息参数缺失。");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(args[index]));
        }
        return matcher.appendTail(result).toString();
    }

    /** 用于启动阶段的模板引用校验。 */
    public boolean contains(String key) {
        return messages.get("zh-CN").containsKey(key);
    }

    /** 只导出页面消息；外部未知 key 已在加载时拒绝，不暴露任意配置内容。 */
    public Map<String, Map<String, String>> browserMessages() {
        var result = new LinkedHashMap<String, Map<String, String>>();
        for (String language : LANGUAGES) {
            var values = new LinkedHashMap<String, String>();
            messages.get(language).forEach((key, value) -> {
                if (!key.startsWith("http.")) values.put(key, value);
            });
            result.put(language, Map.copyOf(values));
        }
        return Map.copyOf(result);
    }

    /**
     * 将Locale归一到内置语言；en/ja使用各自目录，null及其他语言回落zh-CN，不读取系统默认值。
     */
    public static String languageTag(Locale locale) {
        if (locale == null) return "zh-CN";
        return switch (locale.getLanguage()) {
            case "en" -> "en";
            case "ja" -> "ja";
            default -> "zh-CN";
        };
    }

    /**
     * 用严格UTF-8读取properties并拒绝重复key、非法消息名和占位符；流由调用方关闭，返回不可变文本表。
     */
    private static Map<String, String> read(InputStream stream) throws IOException {
        var properties = new Properties() {
            /** 禁止properties默认的后值覆盖前值，重复key必须在加载阶段明确失败。 */
            @Override
            public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("msg 文件包含重复 key。");
                return super.put(key, value);
            }
        };
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        properties.load(new InputStreamReader(stream, decoder));
        var values = new LinkedHashMap<String, String>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.matches("[a-z][a-zA-Z0-9]*(\\.[a-zA-Z0-9]+)+")) {
                throw new IllegalStateException("msg key 格式非法。");
            }
            String value = properties.getProperty(key);
            parameters(value);
            values.put(key, value);
        }
        return Map.copyOf(values);
    }

    /**
     * 要求译文与基准使用相同参数编号集合；不要求顺序或重复次数相同，也不解释格式化表达式。
     */
    private static void validateTranslation(String expected, String actual) {
        if (!parameters(expected).equals(parameters(actual))) {
            throw new IllegalStateException("msg 译文占位符不一致。");
        }
    }

    /**
     * 校验非空白文本只含从零连续编号的占位符并返回编号集合；拒绝其它花括号和超范围编号。
     */
    private static Set<Integer> parameters(String value) {
        if (value.isBlank()) throw new IllegalStateException("msg 消息不能为空白。");
        Matcher matcher = ARGUMENT.matcher(value);
        var indices = new TreeSet<Integer>();
        while (matcher.find()) {
            try {
                indices.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException invalid) {
                // 保留原因类型和堆栈，替换可能包含配置原文的异常消息。
                var sanitized = new NumberFormatException("msg 参数编号超出范围。");
                sanitized.setStackTrace(invalid.getStackTrace());
                throw new IllegalStateException("msg 参数编号超出范围。", sanitized);
            }
        }
        String remainder = matcher.replaceAll("");
        if (remainder.indexOf('{') >= 0 || remainder.indexOf('}') >= 0) {
            throw new IllegalStateException("msg 仅支持编号文本占位符。");
        }
        int next = 0;
        for (int index : indices) {
            if (index != next++) throw new IllegalStateException("msg 参数编号必须从零连续排列。");
        }
        return indices;
    }
}
