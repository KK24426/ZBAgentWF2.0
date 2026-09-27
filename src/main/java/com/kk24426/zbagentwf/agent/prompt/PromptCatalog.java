/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：显式加载 UTF-8 规则快照，查询时不进行文件访问。
 */
package com.kk24426.zbagentwf.agent.prompt;

import com.kk24426.zbagentwf.common.agent.model.Prompt;
import com.kk24426.zbagentwf.common.exception.AgentConfigurationUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 缺文件允许仅启动 Web；使用相应规则时明确拒绝，不构造默认业务或安全规则。 */
public final class PromptCatalog {
    private final Map<String, String> rules;
    private PromptCatalog(Map<String, String> rules) { this.rules = Map.copyOf(rules); }

    /**
     * 在启动阶段读取固定五种UTF-8提示词并保存文本快照，不创建文件或热更新。
     * 缺失、空白及YOUR_占位内容记为未配置；存在但不可读或编码非法则失败。
     * @param directory 相对启动工作目录或绝对的提示词目录
     * @return 只读目录，后续查询不访问磁盘
     * @throws IOException 目录、读取或UTF-8解码失败
     */
    public static PromptCatalog load(Path directory) throws IOException {
        if (!Files.notExists(directory, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(directory)) {
            throw new IOException("提示词目录不可用。");
        }
        var rules = new HashMap<String, String>();
        for (String name : List.of("default", "security", "planning", "development", "review")) {
            Path file = directory.resolve(name + ".txt");
            if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) continue;
            // 固定文件名，不接受请求指定路径；非法 UTF-8、目录或读取失败均明确报错。
            String value = Files.readString(file, StandardCharsets.UTF_8);
            if (value.startsWith("\uFEFF")) value = value.substring(1);
            if (!value.isBlank() && !value.strip().startsWith("YOUR_")) rules.put(name, value);
        }
        return new PromptCatalog(rules);
    }

    /**
     * 读取已加载规则并返回新Prompt；未知、缺失或占位规则抛配置不可用异常，不提供内置替代文本。
     */
    public Prompt require(String name) {
        String value = rules.get(name);
        if (value == null) throw new AgentConfigurationUnavailableException();
        var prompt = new Prompt(); prompt.setPrompt(value); return prompt;
    }
}
