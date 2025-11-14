package com.jjg.game.common.concurrent;


import cn.hutool.core.util.RandomUtil;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Disruptor 版本的 PlayerExecutorGroup：
 * - 按 bindId 映射到 slot（slot 是 Disruptor 单消费者）
 * - 提供 tryPublish（非阻塞）和 publishWithFallback（Netty-friendly）接口
 * - 提供监控 API：slot pending, processed, avgLatency, rejected
 */
public class PlayerExecutorGroupDisruptor {

    private static final Logger log = LoggerFactory.getLogger(PlayerExecutorGroupDisruptor.class);

    private final PlayerWorker[] workers;
    private final int slotCount;
    private final int bufferSize;
    /** 当 publish 失败时，将任务交给 fallbackExecutor 执行，避免阻塞 Netty I/O 线程 */
    private ExecutorService fallbackExecutor;

    private final static PlayerExecutorGroupDisruptor defaultExecutor = new PlayerExecutorGroupDisruptor(0, 0, "default");

    /**
     * 构造
     * @param slotCount 要创建的槽位数（建议为2的幂），如果 <=0 则自动按 cores*2
     * @param bufferSize 每个环形缓冲区的大小（必须为2的幂）
     * @param threadNamePrefix 线程名前缀
     */
    public PlayerExecutorGroupDisruptor(int slotCount, int bufferSize, String threadNamePrefix) {
        int cores = Runtime.getRuntime().availableProcessors();
        int sc = slotCount > 0 ? slotCount : cores * 2;
        // round up to power of two for fast masking if desired
        this.slotCount = nextPowerOfTwo(sc);
        this.bufferSize = nextPowerOfTwo(bufferSize <= 0 ? 1024 : bufferSize);

        this.workers = new PlayerWorker[this.slotCount];
        for (int i = 0; i < this.slotCount; i++) {
            this.workers[i] = new PlayerWorker(threadNamePrefix + "-" + i, this.bufferSize);
        }
        log.info("PlayerExecutorGroupDisruptor initialized: slots={}, bufferSize={}",
                this.slotCount, this.bufferSize);
    }

    public static PlayerExecutorGroupDisruptor getDefaultExecutor() {
        return defaultExecutor;
    }

    /**
     * 构造
     * @param slotCount 要创建的槽位数（建议为2的幂），如果 <=0 则自动按 cores*4
     * @param bufferSize 每个环形缓冲区的大小（必须为2的幂）
     * @param fallbackExecutor 当 ring full 时的降级执行器（可传 null 使用默认）
     * @param threadNamePrefix 线程名前缀
     */
    public PlayerExecutorGroupDisruptor(int slotCount, int bufferSize,
                                        ExecutorService fallbackExecutor, String threadNamePrefix) {
        int cores = Runtime.getRuntime().availableProcessors();
        int sc = slotCount > 0 ? slotCount : cores * 4;
        // round up to power of two for fast masking if desired
        this.slotCount = nextPowerOfTwo(sc);
        this.bufferSize = nextPowerOfTwo(bufferSize <= 0 ? 1024 : bufferSize);

        this.workers = new PlayerWorker[this.slotCount];
        for (int i = 0; i < this.slotCount; i++) {
            this.workers[i] = new PlayerWorker(threadNamePrefix + "-" + i, this.bufferSize);
        }
        if (fallbackExecutor != null) {
            this.fallbackExecutor = fallbackExecutor;
        } else {
            // default bounded thread pool for fallback: protect system if many rejects
            this.fallbackExecutor = new ThreadPoolExecutor(
                    Math.max(2, cores / 2),
                    Math.max(2, cores),
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1024),
                    r -> new Thread(r, threadNamePrefix + "-fallback"),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
        log.info("PlayerExecutorGroupDisruptor initialized: slots={}, bufferSize={}, fallbackExec={}",
                this.slotCount, this.bufferSize, this.fallbackExecutor);
    }

    /** 默认 murmur3 hash，可替换为自定义 hash 函数 */
    @SuppressWarnings("UnstableApiUsage")
    private int calcSlot(long id) {
        int hash = Hashing.murmur3_32().hashLong(id).asInt();
        return hash & (slotCount - 1); // requires slotCount power-of-two
    }

    public PlayerWorker getPlayerWorker(long bindId) {
        int idx = calcSlot(bindId);
        if (idx >= this.workers.length) {
            return null;
        }
        return this.workers[idx];
    }

    /**
     * 非阻塞尝试发布（fast path）。返回 true 表示已入队（将在对应 slot 单线程执行）；
     * 返回 false 表示 ring 已满，调用者需要走降级逻辑（不会阻塞）
     */
    public boolean tryPublish(long bindId, int msgId, BaseHandler<?> task) {
        int idx;
        if (bindId == 0) {
            idx = RandomUtil.randomInt(workers.length);
        } else {
            idx = calcSlot(bindId);
        }
        boolean tryPublish = workers[idx].tryPublish(msgId, task);
        if (!tryPublish) {
            log.error("tryPublish is rejected bindId:{} msgId:{} param:{}", bindId, msgId, task.getHandlerParam());
        }
        return tryPublish;
    }

    /**
     * Netty 最佳实践入口：尝试非阻塞发布；如果失败（队列满），安全地把任务交给 fallbackExecutor 执行，
     * 从而**不阻塞 Netty I/O 线程**也不破坏序列性（降级执行时序列性无法保证，需业务保证幂等或允许降级）。
     *
     */
    public void publishWithFallback(long bindId, int msgId, BaseHandler<?> task) {
        if (!tryPublish(bindId, msgId, task)) {
            if (fallbackExecutor == null) {
                return;
            }
            // fallback: submit to fallbackExecutor to avoid blocking IO threads
            try {
                fallbackExecutor.execute(() -> {
                    try {
                        // second attempt: try blocking publish (not on IO thread)
                        int idx = calcSlot(bindId);
                        workers[idx].publishBlocking(msgId, task);
                    } catch (Throwable t) {
                        // last resort: run directly
                        try {
                            task.action();
                        } catch (Throwable tt) {
                            log.error("fallback direct run error", tt);
                        }
                    }
                });
            } catch (RejectedExecutionException rex) {
                // fallback pool is saturated, as last resort run in caller (NOT recommended in Netty IO thread)
                log.warn("fallbackExecutor rejected, executing in caller thread");
                try {
                    task.action();
                } catch (Throwable t) {
                    log.error("last-resort run error", t);
                }
            }
        }
    }

    // -------------------- Monitoring APIs --------------------

    public int getSlotCount() {
        return slotCount;
    }

    /** pending items in slot (approx) */
    public long getPendingBySlot(int slot) {
        return workers[slot].getPendingCount();
    }

    public long getProcessedBySlot(int slot) {
        return workers[slot].getProcessedCount();
    }

    public double getAvgLatencyMillisBySlot(int slot) {
        return workers[slot].getAvgLatencyMillis();
    }

    public long getRejectedBySlot(int slot) {
        return workers[slot].getRejectedCount();
    }

    /** 简单汇总 */
    public String getOverview() {
        StringBuilder sb = new StringBuilder();
        sb.append("slots=").append(slotCount).append(", buffer=").append(bufferSize).append("\n");
        for (int i = 0; i < slotCount; i++) {
            sb.append("slot[").append(i).append("]: pending=").append(getPendingBySlot(i))
                    .append(", processed=").append(getProcessedBySlot(i))
                    .append(", avgLat(ms)=").append(String.format("%.3f", getAvgLatencyMillisBySlot(i)))
                    .append(", rejected=").append(getRejectedBySlot(i)).append("\n");
        }
        return sb.toString();
    }

    /** 优雅停机 */
    public void shutdownGracefully(long awaitMsPerSlot) {
        for (PlayerWorker w : workers) {
            w.shutdown(awaitMsPerSlot);
        }
        // shutdown fallback executor
        if (this.fallbackExecutor != null) {
            this.fallbackExecutor.shutdown();
            try {
                this.fallbackExecutor.awaitTermination(Math.max(1000, awaitMsPerSlot), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // -------------------- util --------------------
    private int nextPowerOfTwo(int v) {
        if (v <= 0) return 1;
        int n = v - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }
}
