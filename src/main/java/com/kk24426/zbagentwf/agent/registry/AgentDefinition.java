/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存模型三元组和独立的本机执行配置。
 */
package com.kk24426.zbagentwf.agent.registry;

import com.kk24426.zbagentwf.common.agent.bean.AgentBean;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;

/** 技术配置；模型元数据与 CLI 实现、路径、模型参数分开保存。 */
public record AgentDefinition(String brand, String name, String ver, String type,
        String executable, String model, Duration timeout, Boolean enabled) {
    public AgentDefinition {
        // 配置条目必须完整且字段合法；普通 AgentBean 的原样存取规则不受这里的校验影响。
        new Key(brand, name, ver);
        required(type); required(executable); required(model);
        if (!type.equals("codex")) throw new IllegalArgumentException("当前仅支持 codex 执行实现。");
        if (enabled == null || timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Agent 必须显式配置启用状态和正值超时。");
        }
        // 提前拒绝无法表示为纳秒的时长，避免第一次真实调用才因超时换算溢出而失败。
        timeout.toNanos();
        try { executable = Path.of(executable).toAbsolutePath().normalize().toString(); }
        catch (InvalidPathException failure) {
            // 原异常消息携带输入路径；替换成安全异常并保留栈，不能把原始路径挂入 cause。
            var safe = new InvalidPathException("[已隐藏]", "Agent 可执行文件路径格式非法", failure.getIndex());
            safe.setStackTrace(failure.getStackTrace());
            throw safe;
        }
    }

    public Key key() { return new Key(brand, name, ver); }
    public Path path() { return Path.of(executable); }
    public AgentBean bean() { return key().bean(); }

    private static void required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Agent 配置必填字段不能为空。");
    }

    /** 精确值键，不持有调用方可修改的 Bean。 */
    public record Key(String brand, String name, String ver) {
        // 不裁剪、不改变大小写、不填默认版本，三个字段共同表达用户选择的精确身份。
        public Key { required(brand); required(name); required(ver); }
        public static Key from(AgentBean bean) {
            if (bean == null) throw new IllegalArgumentException("必须指定 Agent 模型。");
            return new Key(bean.getBrand(), bean.getName(), bean.getVer());
        }
        public AgentBean bean() {
            var bean = new AgentBean();
            bean.setBrand(brand); bean.setName(name); bean.setVer(ver);
            return bean;
        }
    }
}
