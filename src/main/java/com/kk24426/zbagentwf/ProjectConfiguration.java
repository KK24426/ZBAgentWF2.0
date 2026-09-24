/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：在 Spring 初始化时读取并校验必填项目根目录配置。
 */
package com.kk24426.zbagentwf;

import com.kk24426.zbagentwf.common.project.bean.ProjectSettings;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** 只读取配置与目录元数据；不创建项目目录，不初始化业务项目。 */
@Configuration(proxyBeanMethods = false)
public class ProjectConfiguration {
    @Bean
    ProjectSettings projectSettings(Environment environment) {
        String configured = environment.getProperty("zb.project.root");
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("必须显式配置非空的 zb.project.root。");
        }
        Path root;
        try {
            root = Path.of(configured).toAbsolutePath().normalize();
        } catch (InvalidPathException failure) {
            // 原异常的 input 含本地配置；保留异常类型、位置和完整堆栈，替换原值及原因文本。
            var sanitized = new InvalidPathException("[已隐藏]", "路径格式非法", failure.getIndex());
            sanitized.setStackTrace(failure.getStackTrace());
            throw new IllegalStateException("zb.project.root 路径格式非法。", sanitized);
        }
        // 不跟随链接判断是否已存在，再按目标判断目录；悬空链接也不能作为根目录。
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(root)) {
            throw new IllegalStateException("zb.project.root 已存在但不是目录。");
        }
        return new ProjectSettings(root);
    }
}
