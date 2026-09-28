/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证版本注册、工厂复用、项目角色和共享关闭。
 */
package com.kk24426.zbagentwf.agent.registry;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.codex.CodexFixtureSupport;
import com.kk24426.zbagentwf.agent.project.ProjectDomainImpl;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.common.agent.model.*;
import com.kk24426.zbagentwf.common.project.model.*;
import com.kk24426.zbagentwf.common.project.config.ProjectSettings;
import com.kk24426.zbagentwf.user.agent.api.ProjectAgentExecutor;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证精确模型注册及按项目缓存的绑定隔离。 */
class AgentRegistryTest {
    @TempDir Path temp;

    @Test
    void exactVersionLookupReturnsCopiesAndRefreshChecksFilesWithoutModelCalls() throws Exception {
        Path executable = temp.resolve("fixture.exe");
        var definition = definition("one", executable.toString());
        var catalog = new AgentCatalog(List.of(definition));
        catalog.initialize();
        assertThrows(IllegalStateException.class, catalog::getActiveAgent);
        Files.copy(javaExecutable(), executable);
        assertTrue(executable.toFile().setExecutable(true));
        var models = catalog.refreshAgentList();
        models.getFirst().setVer("changed");
        assertEquals("one", catalog.getActiveAgent("provider", "model", "one").getVer());
        assertThrows(IllegalStateException.class, () -> catalog.getActiveAgent("provider", "model", "changed"));
        Files.delete(executable);
        assertThrows(IllegalStateException.class, catalog::refreshAgentList);
        assertThrows(IllegalStateException.class, () -> catalog.getActiveAgent("provider", "model", "one"));
        assertThrows(IllegalArgumentException.class, () -> new AgentCatalog(List.of(definition, definition)));
    }

    @Test
    void rootDirectoriesAndDisabledEntriesDoNotPreventValidRegistrations() throws Exception {
        String root = temp.toAbsolutePath().getRoot().toString();
        var disabled = new AgentDefinition("provider", "model", "disabled", "codex", root,
                "fixture-selector", Duration.ofMinutes(2), false);
        var catalog = new AgentCatalog(List.of(definition("root", root), disabled,
                definition("directory", Files.createDirectory(temp.resolve("directory.exe")).toString()),
                definition("valid", javaExecutable().toString())));
        assertDoesNotThrow(catalog::initialize);
        assertEquals(List.of("valid"), catalog.getActiveAgent().stream().map(AgentBean::getVer).toList());
        assertEquals(List.of("valid"), catalog.refreshAgentList().stream().map(AgentBean::getVer).toList());
    }

    /** 工厂按项目复用，其他项目隔离，绑定后的think变化拒绝重新绑定。 */
    @Test void projectFactoryReusesCanonicalIdentityAndRejectsChangedBindings() throws Exception {
        try(var env=new ProjectFixtureSupport(temp.resolve("projects"),"success","success")) {
            var p=env.single();var first=env.factory.getExecutor(p);assertSame(first,env.factory.getExecutor(p));
            var other=env.single();assertNotSame(first,env.factory.getExecutor(other));
            p.getDevelopmentAgent().setThink("changed");assertThrows(IllegalArgumentException.class,() -> env.factory.getExecutor(p));
        }
    }
    /** 使用当前JVM的原生程序路径，只验证文件注册而不调用真实模型。 */
    private static Path javaExecutable() {return Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");}
    /** 创建测试专用模型配置，版本独立且超时固定为八秒。 */
    private static AgentDefinition definition(String version,String executable) {return new AgentDefinition("provider","model",version,"codex",executable,"fixture",Duration.ofSeconds(8),true);}
}
