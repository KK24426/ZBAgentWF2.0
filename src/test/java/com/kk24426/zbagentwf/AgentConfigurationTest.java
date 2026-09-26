/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证外部模型配置绑定、覆盖、错误以及接口装配。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.user.agent.userif.*;
import com.kk24426.zbagentwf.user.project.userif.ProjectUserif;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class AgentConfigurationTest {
    @TempDir Path temp;

    @Test
    void externalFileLoadsAndHigherPriorityPropertiesOverrideWithoutRunningCliOrCreatingProjects() throws Exception {
        Path file = temp.resolve("agents.properties");
        Properties values = properties();
        try (var out = Files.newOutputStream(file)) { values.store(out, "fixture"); }
        var holder = new AgentExecFactory[1];
        var model = new com.kk24426.zbagentwf.common.agent.bean.AgentBean[1];
        runner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=optional:file:" + temp.resolve("absent.properties").toUri(),
                        "spring.config.import=" + file.toUri(), "zb.agents[0].ver=overridden")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var catalog = context.getBean(AgentBase.class);
                    model[0] = catalog.getActiveAgent("fixture-provider", "fixture-model", "overridden");
                    assertNotNull(context.getBean(ProjectUserif.class));
                    holder[0] = context.getBean(AgentExecFactory.class);
                    assertSame(holder[0].getExecutor(model[0]), holder[0].getExecutor(model[0]));
                    assertFalse(Files.exists(temp.resolve("projects")));
                });
        assertThrows(IllegalStateException.class, () -> holder[0].getExecutor(model[0]));
    }

    @Test
    void missingRegistrationsPermitWebCompositionButLookupExplicitlyFails() {
        runner().run(context -> {
            assertNull(context.getStartupFailure());
            assertThrows(IllegalStateException.class, () -> context.getBean(AgentBase.class).getActiveAgent());
        });
    }

    @Test
    void malformedMissingAndDuplicateEntriesFailInitialization() {
        for (String invalid : List.of("zb.agents[0].timeout=0s", "zb.agents[0].timeout=invalid",
                "zb.agents[0].brand=", "zb.agents[0].type=unsupported", "zb.agents[0].enabled=invalid")) {
            runner().withPropertyValues(asArguments(properties())).withPropertyValues(invalid)
                    .run(context -> assertNotNull(context.getStartupFailure(), invalid));
        }
        Properties missing = properties(); missing.remove("zb.agents[0].ver");
        runner().withPropertyValues(asArguments(missing)).run(context -> assertNotNull(context.getStartupFailure()));
        Properties duplicates = properties();
        properties().forEach((key, value) -> duplicates.put(key.toString().replace("[0]", "[1]"), value));
        runner().withPropertyValues(asArguments(duplicates)).run(context -> assertNotNull(context.getStartupFailure()));
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withUserConfiguration(ProjectConfiguration.class, AgentConfiguration.class)
                .withPropertyValues("zb.project.root=" + temp.resolve("projects"));
    }

    @Test
    void systemPropertiesOverrideEnvironmentWhichOverridesFileFields() throws Exception {
        Path file = temp.resolve("overrides.properties");
        try (var out = Files.newOutputStream(file)) { properties().store(out, "fixture"); }
        var base = runner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withInitializer(context -> context.getEnvironment().getPropertySources().addAfter("systemProperties",
                        new SystemEnvironmentPropertySource("systemEnvironment", Map.of("ZB_AGENTS_0_VER", "environment"))))
                .withPropertyValues("spring.config.location=optional:file:" + temp.resolve("absent.properties").toUri(),
                        "spring.config.import=" + file.toUri());
        base.run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals("environment", context.getBean(AgentBase.class).getActiveAgent().getFirst().getVer());
        });
        base.withSystemProperties("zb.agents[0].ver=system").run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals("system", context.getBean(AgentBase.class).getActiveAgent().getFirst().getVer());
        });
    }
    private Properties properties() {
        var values = new Properties();
        values.setProperty("zb.agents[0].brand", "fixture-provider");
        values.setProperty("zb.agents[0].name", "fixture-model");
        values.setProperty("zb.agents[0].ver", "one");
        values.setProperty("zb.agents[0].type", "codex");
        values.setProperty("zb.agents[0].executable", Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString());
        values.setProperty("zb.agents[0].model", "fixture-selector");
        values.setProperty("zb.agents[0].timeout", "2m");
        values.setProperty("zb.agents[0].enabled", "true");
        return values;
    }
    private String[] asArguments(Properties properties) {
        return properties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }
}
