/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-26
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：管理共享执行额度、完成诊断缓存及有界关闭。
 */
package com.kk24426.zbagentwf.agent.runtime;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

/** 一个应用工厂共享一个资源实例；构造不启动线程，执行器和规划器共同使用票据。 */
public final class ExecutionResources implements AutoCloseable {
    private static final int MAX_ACTIVE = 4;
    private final int diagnosticLimit;
    private final long retentionNanos;
    private final LongSupplier time;
    private final Map<String, Ticket> active = new HashMap<>();
    private final LinkedHashMap<String, Diagnostic> completed = new LinkedHashMap<>();
    private final Set<Object> closedOwners = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ScheduledThreadPoolExecutor expiry = new ScheduledThreadPoolExecutor(1,
            Thread.ofPlatform().daemon(true).name("agent-diagnostic-expiry").factory());
    private boolean closed;
    private long shutdownDeadline;

    public ExecutionResources() { this(256, Duration.ofMinutes(30), System::nanoTime); }

    // 可控时钟仅用于验证到期行为；生产使用单调时钟。
    ExecutionResources(int diagnosticLimit, Duration retention, LongSupplier time) {
        if (diagnosticLimit < 1 || retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("诊断容量与保留时间必须为正。");
        }
        this.diagnosticLimit = diagnosticLimit;
        this.retentionNanos = retention.toNanos();
        this.time = Objects.requireNonNull(time);
        expiry.setRemoveOnCancelPolicy(true);
    }

    /** 提交前预留；没有排队，也不在拒绝时产生完成回调。 */
    public synchronized Ticket reserve(Object owner) {
        Objects.requireNonNull(owner);
        if (closed || closedOwners.contains(owner) || active.size() >= MAX_ACTIVE) {
            throw new RejectedExecutionException("Agent 已关闭或共享执行额度已满。");
        }
        var ticket = new Ticket(UUID.randomUUID().toString(), owner);
        active.put(ticket.id, ticket);
        return ticket;
    }

    public synchronized String diagnostic(Object owner, String id) {
        prune();
        Ticket running = active.get(id);
        if (running != null && running.owner == owner && !closed && !closedOwners.contains(owner)) return "";
        Diagnostic saved = completed.get(id);
        return saved != null && saved.owner == owner ? saved.text : null;
    }

    private void prune() {
        long now = time.getAsLong();
        var entries = completed.values().iterator();
        while (entries.hasNext()) {
            Diagnostic value = entries.next();
            if (now - value.finishedAt >= retentionNanos) {
                value.expiry.cancel(false);
                entries.remove();
            }
        }
    }

    private synchronized void finish(Ticket ticket, String text) {
        if (!active.remove(ticket.id, ticket)) return;
        if (text != null && !closed && !closedOwners.contains(ticket.owner)) {
            prune();
            while (completed.size() >= diagnosticLimit) {
                Diagnostic oldest = completed.pollFirstEntry().getValue();
                oldest.expiry.cancel(false);
            }
            var saved = new Diagnostic(ticket.owner, text, time.getAsLong());
            completed.put(ticket.id, saved);
            saved.expiry = expiry.schedule(() -> expire(ticket.id, saved), retentionNanos, TimeUnit.NANOSECONDS);
        }
        notifyAll();
    }

    private synchronized void expire(String id, Diagnostic saved) { completed.remove(id, saved); }

    /** 关闭事件先调用本方法；不等待工作完成，唯一截止时间从此刻开始。 */
    public synchronized void beginShutdown() {
        if (closed) return;
        closed = true;
        shutdownDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        completed.values().forEach(value -> value.expiry.cancel(false));
        completed.clear();
        expiry.shutdownNow();
        active.values().forEach(Ticket::interrupt);
    }

    /** 单个低层执行器关闭不影响同工厂的其他模型；正常应用由工厂统一关闭。 */
    public void closeOwner(Object owner) {
        synchronized (this) {
            closedOwners.add(owner);
            completed.values().removeIf(value -> {
                if (value.owner != owner) return false;
                value.expiry.cancel(false);
                return true;
            });
            active.values().stream().filter(ticket -> ticket.owner == owner).forEach(Ticket::interrupt);
        }
        await(owner, System.nanoTime() + TimeUnit.SECONDS.toNanos(5));
    }

    @Override public void close() {
        beginShutdown();
        await(null, shutdownDeadline);
    }

    private void await(Object owner, long deadline) {
        boolean interrupted = Thread.interrupted();
        synchronized (this) {
            while (active.values().stream().anyMatch(ticket -> (owner == null || ticket.owner == owner)
                    && ticket.thread != Thread.currentThread())) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                try { TimeUnit.NANOSECONDS.timedWait(this, remaining); }
                catch (InterruptedException failure) { interrupted = true; }
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    /** 票据可先预留再附着线程；关闭后晚附着也会收到中断。 */
    public final class Ticket implements AutoCloseable {
        private final String id;
        private final Object owner;
        private Thread thread;

        private Ticket(String id, Object owner) { this.id = id; this.owner = owner; }
        public String id() { return id; }

        public void attach(Thread thread) {
            synchronized (ExecutionResources.this) {
                if (this.thread != null || active.get(id) != this) throw new IllegalStateException("票据不能重复附着。");
                this.thread = Objects.requireNonNull(thread);
                if (closed || closedOwners.contains(owner)) interrupt();
            }
        }

        public void checkRunning() {
            synchronized (ExecutionResources.this) {
                if (closed || closedOwners.contains(owner) || active.get(id) != this) {
                    throw new RejectedExecutionException("Agent 正在关闭。");
                }
            }
        }

        private void interrupt() { if (thread != null) thread.interrupt(); }
        public void complete(String diagnostic) { finish(this, Objects.requireNonNullElse(diagnostic, "")); }
        /** 拒绝或同步规划结束只归还额度，不伪造公开执行诊断记录。 */
        @Override public void close() { finish(this, null); }
    }

    private static final class Diagnostic {
        final Object owner;
        final String text;
        final long finishedAt;
        ScheduledFuture<?> expiry;
        Diagnostic(Object owner, String text, long finishedAt) {
            this.owner = owner; this.text = text; this.finishedAt = finishedAt;
        }
    }
}
