/*
 * 创建日期：2026-09-26
 * 更新日期：2026-09-27
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
    // 运行名额和完成诊断分开保存：容量淘汰只作用于已完成记录，不能释放正在执行的名额。
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
        // 容量淘汰和主动关闭会取消到期任务，同时移出调度队列，避免队列持续持有诊断对象。
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

    /** 按执行器身份隔离诊断；运行中尚无最终 stderr 时返回空串，未知或过期返回 null。 */
    public synchronized String diagnostic(Object owner, String id) {
        // 定时器可能尚未调度，读取前仍按单调时钟核验到期，避免返回超期文本。
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
        // 移除成功的一方才执行后续动作，让拒绝清理、正常完成和重复关闭至多归还一次名额。
        if (!active.remove(ticket.id, ticket)) return;
        // null 表示只归还票据（规划或受理失败）；关闭期间的迟到结果不能重新填充诊断缓存。
        if (text != null && !closed && !closedOwners.contains(ticket.owner)) {
            prune();
            // LinkedHashMap 按完成顺序插入；超容量先淘汰最早完成记录，不因查询而延长保留期。
            while (completed.size() >= diagnosticLimit) {
                Diagnostic oldest = completed.pollFirstEntry().getValue();
                oldest.expiry.cancel(false);
            }
            var saved = new Diagnostic(ticket.owner, text, time.getAsLong());
            completed.put(ticket.id, saved);
            saved.expiry = expiry.schedule(() -> expire(ticket.id, saved), retentionNanos, TimeUnit.NANOSECONDS);
        }
        // 等待关闭的线程关心票据归还，不等待工作线程或业务回调的整个生命周期。
        notifyAll();
    }

    private synchronized void expire(String id, Diagnostic saved) { completed.remove(id, saved); }

    /** 关闭事件先调用本方法；不等待工作完成，唯一截止时间从此刻开始。 */
    public synchronized void beginShutdown() {
        if (closed) return;
        closed = true;
        // 截止时间只在首次关闭时确定；Spring 关闭事件与 Bean 销毁不能各重新等待 20 秒。
        shutdownDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        completed.values().forEach(value -> value.expiry.cancel(false));
        completed.clear();
        expiry.shutdownNow();
        active.values().forEach(Ticket::interrupt);
    }

    /** 单个低层执行器关闭不影响同工厂的其他模型；正常应用由工厂统一关闭。 */
    public void closeOwner(Object owner) {
        synchronized (this) {
            // 先封闭该归属，再中断已有票据，确保清理期间无法重新提交或写回诊断。
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
        // 先清除已有中断以完成有限回收，退出时再恢复，既不忙循环也不吞掉调用方中断信号。
        boolean interrupted = Thread.interrupted();
        synchronized (this) {
            // 同步规划可能运行在长期调用线程上，只等待它的票据；重入关闭时排除当前线程，
            // 否则它必须先等自己返回才能归还票据，会造成自等待。
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
                // reserve 与线程附着之间可能发生关闭；晚附着也要收到中断，不能遗漏此窗口。
                if (closed || closedOwners.contains(owner)) interrupt();
            }
        }

        /** 在启动实际工作前复核票据，处理线程已登记但还未执行时发生的关闭。 */
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
