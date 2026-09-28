/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.2
 * 功能概要：保存项目根目录并将业务UUID映射到固定local身份下的独立目录。
 */
package com.kk24426.zbagentwf.common.project.config;

import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;

/** 保存项目根目录并将业务UUID映射到固定local身份下的独立目录。 */
public final class ProjectSettings {
    private final Path rootDirectory;
    /** 保存已验证根目录；构造不访问文件系统。 */
    public ProjectSettings(Path rootDirectory) { this.rootDirectory = rootDirectory.toAbsolutePath().normalize(); }
    public Path getRootDirectory() { return rootDirectory; }
    /** 仅接受规范UUID，避免路径片段、绝对路径或目录穿越；不创建目录。 */
    public Path path(String projectId) {
        if (projectId == null || !UUID.fromString(projectId).toString().equals(projectId))
            throw new IllegalArgumentException("项目标识必须是规范UUID。");
        return rootDirectory.resolve("local").resolve(projectId);
    }
    /** 新建全新项目目录；固定身份目录若被链接到根目录之外则拒绝。 */
    public Path create(String projectId) throws IOException {
        Path expected = path(projectId);
        Files.createDirectories(rootDirectory);
        Path root = rootDirectory.toRealPath();
        Path user = rootDirectory.resolve("local");
        Files.createDirectories(user);
        if (!user.toRealPath().equals(root.resolve("local"))) throw new IOException("用户目录不允许重定向。");
        Files.createDirectory(expected);
        return directory(projectId);
    }
    /** 复核真实路径与预期的root/local/UUID完全一致，拒绝子目录链接替换和越界。 */
    public Path directory(String projectId) {
        try {
            Path actual = path(projectId).toRealPath();
            Path expected = rootDirectory.toRealPath().resolve("local").resolve(projectId);
            if (!Files.isDirectory(actual) || !actual.equals(expected)) throw new IOException("目录归属无效。");
            return actual;
        } catch (IOException failure) { throw new IllegalArgumentException("项目目录不可用。", failure); }
    }
}
