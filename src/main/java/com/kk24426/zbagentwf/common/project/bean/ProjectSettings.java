/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存初始化时读取的全局项目根目录，不创建目录。
 */
package com.kk24426.zbagentwf.common.project.bean;

import java.nio.file.Path;

/** 只读技术配置；业务项目目录保存在 Project 中，不输出配置值到日志。 */
public final class ProjectSettings {
    private final Path rootDirectory;

    public ProjectSettings(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    /** 返回初始化时规范化后的绝对根目录。 */
    public Path getRootDirectory() {
        return rootDirectory;
    }
}
