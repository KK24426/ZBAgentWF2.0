/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证资源额度、诊断生命周期与关闭登记竞态。
 */
package com.kk24426.zbagentwf.agent.runtime;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ExecutionResourcesTest {
    @Test
    void capacityIsSharedAndReleasedOnRejectedSubmissionOrCompletion() {
        try (var resources = new ExecutionResources()) {
            var tickets = new ArrayList<ExecutionResources.Ticket>();
            for (int i = 0; i < 4; i++) tickets.add(resources.reserve(new Object()));
            assertThrows(RejectedExecutionException.class, () -> resources.reserve(new Object()));
            tickets.removeFirst().close();
            var next = resources.reserve(new Object());
            next.complete("done");
            tickets.forEach(ExecutionResources.Ticket::close);
            try (var again = resources.reserve(new Object())) { assertNotNull(again.id()); }
        }
    }

    @Test
    void completedCacheHasGlobalCapacityTtlAndOwnerIsolationWhileActiveEntriesSurvive() {
        var time = new AtomicLong();
        Object first = new Object(); Object second = new Object();
        try (var resources = new ExecutionResources(256, Duration.ofMinutes(30), time::get)) {
            var active = resources.reserve(first);
            String oldest = null;
            String newest = null;
            for (int i = 0; i < 257; i++) {
                var ticket = resources.reserve(i % 2 == 0 ? first : second);
                if (i == 0) oldest = ticket.id();
                newest = ticket.id(); ticket.complete("diagnostic " + i);
            }
            assertNull(resources.diagnostic(first, oldest));
            assertEquals("diagnostic 256", resources.diagnostic(first, newest));
            assertNull(resources.diagnostic(second, newest));
            assertEquals("", resources.diagnostic(first, active.id()));
            time.set(Duration.ofMinutes(30).toNanos() - 1);
            assertNotNull(resources.diagnostic(first, newest));
            time.incrementAndGet();
            assertNull(resources.diagnostic(first, newest));
            assertEquals("", resources.diagnostic(first, active.id()));
            active.complete("completed later");
            assertEquals("completed later", resources.diagnostic(first, active.id()));
        }
    }

    @Test
    void shutdownCatchesLateAttachmentAndWaitsForTicketNotLongLivedCallerThread() throws Exception {
        var resources = new ExecutionResources();
        var ticket = resources.reserve(new Object());
        var releaseCaller = new CountDownLatch(1);
        var completed = new CompletableFuture<Boolean>();
        resources.beginShutdown();
        Thread caller = Thread.ofVirtual().unstarted(() -> {
            ticket.attach(Thread.currentThread());
            boolean interrupted = Thread.interrupted();
            assertThrows(RejectedExecutionException.class, ticket::checkRunning);
            ticket.close();
            completed.complete(interrupted);
            try { releaseCaller.await(); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
        });
        try {
            caller.start();
            assertTrue(completed.get(5, TimeUnit.SECONDS));
            assertTimeoutPreemptively(Duration.ofSeconds(2), resources::close);
            assertTrue(caller.isAlive(), "关闭不得等待长期调用线程结束");
            assertThrows(RejectedExecutionException.class, () -> resources.reserve(new Object()));
        } finally { releaseCaller.countDown(); resources.close(); }
    }

    @Test
    void closeOwnerPreventsLateDiagnosticWritesAndDoesNotStopOtherOwners() {
        Object owner = new Object(); Object other = new Object();
        try (var resources = new ExecutionResources()) {
            var ticket = resources.reserve(owner);
            ticket.attach(Thread.currentThread());
            resources.closeOwner(owner);
            assertTrue(Thread.interrupted());
            ticket.complete("must not reappear");
            assertNull(resources.diagnostic(owner, ticket.id()));
            assertThrows(RejectedExecutionException.class, () -> resources.reserve(owner));
            try (var allowed = resources.reserve(other)) { assertNotNull(allowed.id()); }
        }
    }
}
