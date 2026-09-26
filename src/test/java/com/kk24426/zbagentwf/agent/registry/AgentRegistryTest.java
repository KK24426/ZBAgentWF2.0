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
import com.kk24426.zbagentwf.agent.project.ProjectUserifImpl;
import com.kk24426.zbagentwf.agent.runtime.ExecutionResources;
import com.kk24426.zbagentwf.common.agent.bean.*;
import com.kk24426.zbagentwf.common.project.bean.*;
import com.kk24426.zbagentwf.user.agent.userif.AgentExecutor;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void factoryUsesImmutableKeysAndOneInstanceForConcurrentRequests() throws Exception {
        var first = definition("one", javaExecutable().toString());
        var second = definition("two", javaExecutable().toString());
        var catalog = new AgentCatalog(List.of(first, second)); catalog.initialize();
        var created = new AtomicInteger();
        try (var resources = new ExecutionResources(); var factory = new AgentExecFactoryImpl(catalog, resources, value -> {
            created.incrementAndGet(); return CodexFixtureSupport.client("success", value.timeout(), value.model());
        }); var callers = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<AgentExecutor>>();
            for (int i = 0; i < 20; i++) futures.add(callers.submit(() -> factory.getExecutor(first.bean())));
            AgentExecutor shared = futures.getFirst().get(5, TimeUnit.SECONDS);
            for (var future : futures) assertSame(shared, future.get(5, TimeUnit.SECONDS));
            var mutable = first.bean(); factory.getExecutor(mutable); mutable.setVer("two");
            assertNotSame(shared, factory.getExecutor(mutable));
            assertEquals(2, created.get());
            assertSame(shared, factory.getExecutor(first.bean()));
            assertThrows(IllegalArgumentException.class, () -> factory.getExecutor(new AgentBean()));
            assertThrows(IllegalStateException.class, () -> factory.getExecutor(definition("unknown", javaExecutable().toString()).bean()));
        }
    }

    @Test
    void projectsBindThreeModelsAndDispatchPlanningAndDevelopmentWithoutRunningReview() throws Exception {
        var plan = definition("plan", javaExecutable().toString());
        var develop = definition("develop", javaExecutable().toString());
        var review = definition("review", javaExecutable().toString());
        var catalog = new AgentCatalog(List.of(plan, develop, review)); catalog.initialize();
        try (var resources = new ExecutionResources(); var factory = fixtureFactory(catalog, resources, "success")) {
            var planner = new AgentRequirementPlanner(factory);
            Path root = temp.resolve("projects");
            var service = new ProjectUserifImpl(new ProjectSettings(root), factory, planner);
            assertThrows(IllegalStateException.class, () -> service.newProject("bad", plan.bean(), develop.bean(),
                    definition("missing", javaExecutable().toString()).bean()));
            assertFalse(Files.exists(root));
            Project first = service.newProject("第一项目", plan.bean(), develop.bean(), review.bean());
            assertSame(factory.getExecutor(plan.bean()), first.getPlanningAgent());
            assertSame(factory.getExecutor(develop.bean()), first.getDevelopmentAgent());
            assertSame(factory.getExecutor(review.bean()), first.getReviewAgent());
            assertEquals("plan", Files.readString(first.getWorkingDirectory().resolve("model-selection")));
            Project second = service.newProject("第二项目", review.bean(), plan.bean(), develop.bean());
            assertEquals("review", Files.readString(second.getWorkingDirectory().resolve("model-selection")));
            assertNotSame(first.getRequirements(), second.getRequirements());
            service.execTask(first, first.getRequirements());
            assertEquals("develop", Files.readString(first.getWorkingDirectory().resolve("model-selection")));
            assertTrue(first.getRequirements().stream().flatMap(value -> value.getTasks().stream())
                    .allMatch(task -> task.getStatus() == TaskStatus.SUCCEEDED && task.getResult().getSummary().contains("develop")));
            assertTrue(second.getRequirements().stream().flatMap(value -> value.getTasks().stream())
                    .allMatch(task -> task.getStatus() == TaskStatus.PENDING));
            first.setPlanningAgent(new AgentExecutor(new AgentBean()) {
                public String exec(Project p, String c, String m, com.kk24426.zbagentwf.user.agent.userif.AgentExecCallback cb) { throw new AssertionError(); }
                public String getStderr(String id) { return null; }
            });
            assertThrows(IllegalArgumentException.class, () -> service.createRequirements(first, "unknown binding"));
        }
    }

    @Test
    void multipleModelsAndPlanningShareLimitAndFactoryShutdownCompletesAcceptedWork() throws Exception {
        var first = definition("first", javaExecutable().toString());
        var second = definition("second", javaExecutable().toString());
        var catalog = new AgentCatalog(List.of(first, second)); catalog.initialize();
        try (var resources = new ExecutionResources(); var factory = fixtureFactory(catalog, resources, "sleep")) {
            AgentExecutor a = factory.getExecutor(first.bean()); AgentExecutor b = factory.getExecutor(second.bean());
            var results = new ArrayList<CompletableFuture<AgentExecResult>>();
            var ids = new ArrayList<String>();
            for (int i = 0; i < 4; i++) {
                var done = new CompletableFuture<AgentExecResult>(); results.add(done);
                ids.add((i % 2 == 0 ? a : b).exec(project("run" + i), "task", "", done::complete));
            }
            assertThrows(RejectedExecutionException.class, () -> b.exec(project("full"), "task", "", ignored -> fail()));
            Project planning = project("planning"); planning.setPlanningAgent(a);
            assertThrows(RejectedExecutionException.class, () -> new AgentRequirementPlanner(factory).plan(planning, "plan"));
            factory.beginShutdown();
            factory.close();
            for (var result : results) assertFalse(result.get(8, TimeUnit.SECONDS).isSuccess());
            assertThrows(IllegalStateException.class, () -> factory.getExecutor(first.bean()));
            assertThrows(RejectedExecutionException.class, () -> a.exec(planning, "task", "", ignored -> fail()));
            for (String id : ids) assertNull(a.getStderr(id));
        }
    }

    private AgentExecFactoryImpl fixtureFactory(AgentCatalog catalog, ExecutionResources resources, String scenario) {
        return new AgentExecFactoryImpl(catalog, resources, value -> CodexFixtureSupport.client(scenario, value.timeout(), value.model()));
    }

    @Test
    void acceptedPlanningConsumesAnExecutionSlotAndShutdownDoesNotWaitForCallerLifetime() throws Exception {
        var definition = definition("planner", javaExecutable().toString());
        var catalog = new AgentCatalog(List.of(definition)); catalog.initialize();
        var callerRelease = new CountDownLatch(1);
        var planned = new CompletableFuture<Void>();
        try (var resources = new ExecutionResources(); var factory = fixtureFactory(catalog, resources, "sleep")) {
            Project project = project("synchronous");
            var executor = factory.getExecutor(definition.bean()); project.setPlanningAgent(executor);
            var planner = new AgentRequirementPlanner(factory);
            var held = new ArrayList<ExecutionResources.Ticket>();
            for (int i = 0; i < 3; i++) held.add(resources.reserve(new Object()));
            Thread caller = Thread.ofVirtual().start(() -> {
                try { planner.plan(project, "task"); planned.completeExceptionally(new AssertionError("planning unexpectedly succeeded")); }
                catch (IllegalStateException expected) { planned.complete(null); }
                Thread.interrupted();
                try { callerRelease.await(); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
            });
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!Files.exists(project.getWorkingDirectory().resolve("started")) && System.nanoTime() < deadline) Thread.sleep(10);
                assertTrue(Files.exists(project.getWorkingDirectory().resolve("started")));
                assertThrows(RejectedExecutionException.class, () -> executor.exec(project, "task", "", ignored -> fail()));
                held.forEach(ExecutionResources.Ticket::close);
                factory.beginShutdown();
                factory.close();
                planned.get(5, TimeUnit.SECONDS);
                assertTrue(caller.isAlive());
            } finally { held.forEach(ExecutionResources.Ticket::close); callerRelease.countDown(); }
        }
    }

    @Test
    void callbackCanCloseItsFactoryAfterDiagnosticsAreAvailable() throws Exception {
        var definition = definition("one", javaExecutable().toString());
        var catalog = new AgentCatalog(List.of(definition)); catalog.initialize();
        try (var resources = new ExecutionResources(); var factory = fixtureFactory(catalog, resources, "success")) {
            var executor = factory.getExecutor(definition.bean());
            var done = new CompletableFuture<Void>();
            executor.exec(project("callback"), "task", "", result -> {
                try {
                    assertNotNull(executor.getStderr(result.getTaskId()));
                    factory.close();
                    assertNull(executor.getStderr(result.getTaskId()));
                    done.complete(null);
                } catch (Throwable failure) { done.completeExceptionally(failure); }
            });
            done.get(10, TimeUnit.SECONDS);
        }
    }
    private AgentDefinition definition(String version, String executable) {
        return new AgentDefinition("provider", "model", version, "codex", executable, version, Duration.ofSeconds(15), true);
    }
    private static Path javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
    }
    private Project project(String name) throws Exception {
        var project = new Project(); project.setProjectId(name); project.setWorkingDirectory(Files.createDirectory(temp.resolve(name)));
        return project;
    }
}
