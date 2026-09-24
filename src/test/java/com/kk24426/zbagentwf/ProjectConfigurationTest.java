/*
 * 创建日期：2026-09-25
 * 更新日期：2026-09-25
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证必填项目根目录、路径规范化及初始化无建目录副作用。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class ProjectConfigurationTest {
    @TempDir Path temp;
    private final ProjectConfiguration configuration = new ProjectConfiguration();

    @Test
    void acceptsExistingAndNonexistentDirectoriesWithoutCreatingAnything() {
        assertEquals(temp.toAbsolutePath().normalize(),
                configuration.projectSettings(environment(temp.toString())).getRootDirectory());
        Path absent = temp.resolve("尚未创建的项目根目录");
        assertEquals(absent, configuration.projectSettings(environment(absent.toString())).getRootDirectory());
        assertFalse(Files.exists(absent));
    }

    @Test
    void relativeDirectoryIsNormalizedAgainstApplicationWorkingDirectory() {
        Path relative = Path.of("target", "project-config-" + UUID.randomUUID(), "child", "..", "root");
        Path actual = configuration.projectSettings(environment(relative.toString())).getRootDirectory();
        assertEquals(relative.toAbsolutePath().normalize(), actual);
        assertTrue(actual.isAbsolute());
        assertFalse(Files.exists(actual));
    }

    @Test
    void missingBlankAndExistingFileAreRejected() throws Exception {
        assertThrows(IllegalStateException.class, () -> configuration.projectSettings(new MockEnvironment()));
        for (String value : new String[]{"", " ", "\t\r\n"}) {
            assertThrows(IllegalStateException.class, () -> configuration.projectSettings(environment(value)));
        }
        Path file = Files.writeString(temp.resolve("file"), "fixture");
        assertThrows(IllegalStateException.class, () -> configuration.projectSettings(environment(file.toString())));
    }

    @Test
    void malformedPathPreservesExceptionStackWithoutExposingConfigurationValue() {
        String value = "private-config-value" + (char) 0;
        var failure = assertThrows(IllegalStateException.class,
                () -> configuration.projectSettings(environment(value)));
        assertInstanceOf(InvalidPathException.class, failure.getCause());
        assertTrue(failure.getCause().getStackTrace().length > 0);
        var trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        assertTrue(trace.toString().contains("ProjectConfiguration"));
        assertFalse(trace.toString().contains("private-config-value"));
    }

    private MockEnvironment environment(String value) {
        return new MockEnvironment().withProperty("zb.project.root", value);
    }
}
