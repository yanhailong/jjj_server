package com.jjg.game.common.concurrent;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单 slot worker：内部是 Disruptor 单消费者
 */
public class PlayerWorker {

    private static final Logger log = LoggerFactory.getLogger(PlayerWorker.class);

    private final int bufferSize;
    private final Disruptor<PlayerEvent> disruptor;
    private final RingBuffer<PlayerEvent> ringBuffer;
    private final long consumerSequence;
    private final AtomicLong processedCount = new AtomicLong();
    private final AtomicLong totalLatencyNanos = new AtomicLong();
    private final AtomicLong rejectedCount = new AtomicLong();

    // 构造：name 用于线程命名，bufferSize 必须是 2 的幂
    public PlayerWorker(String name, int bufferSize) {
        this.bufferSize = bufferSize;
        // WaitStrategy 可以根据延迟/吞吐做选择
        WaitStrategy waitStrategy = new BlockingWaitStrategy(); // 稳定可配置，可换为 SleepingWaitStrategy / YieldingWaitStrategy
        this.disruptor = new Disruptor<>(
                new PlayerEventFactory(),
                bufferSize,
                r -> {
                    Thread t = new Thread(r, name);
                    t.setDaemon(false);
                    return t;
                },
                ProducerType.MULTI,
                waitStrategy
        );

        // 单消费者：事件处理器
        EventHandler<PlayerEvent> handler = (event, sequence, endOfBatch) -> {
            BaseHandler<?> task = event.getTask();
            if (task != null) {
                long start = System.nanoTime();
                try {
                    task.action();
                } catch (Throwable t) {
                    log.error("PlayerWorker task error on msgId {} ", event.getMsgId(), t);
                } finally {
                    long cost = (System.nanoTime() - start) / 1_000_000;
                    if (cost > 100) {
                        log.error("PlayerWorker task error on msgId {} cost {} ", event.getMsgId(), cost);
                    }
                    processedCount.incrementAndGet();
                    // latency from publish->start
                    long latency = start - event.getPublishTimeNanos();
                    totalLatencyNanos.addAndGet(Math.max(0, latency));
                }
            }
            // clear to avoid retaining references
            event.clear();
        };

        disruptor.handleEventsWith(handler);
        disruptor.start();
        this.ringBuffer = disruptor.getRingBuffer();
        // consumerSequence: get the first gating sequence (single consumer)
        this.consumerSequence = disruptor.getRingBuffer().getCursor(); // cursor sequence
    }

    /**
     * 尝试非阻塞发布事件。若发布成功返回 true；若失败（ringBuffer full）返回 false。
     * 这里使用 tryPublishEvent (Disruptor >= 3.4) 来实现无阻塞 publish。
     */
    public boolean tryPublish(int msgId, BaseHandler<?> task) {
        long now = System.nanoTime();
        try {
            boolean ok = ringBuffer.tryPublishEvent((event, seq) -> event.set(msgId, task, now));
            if (!ok) {
                rejectedCount.incrementAndGet();
            }
            return ok;
        } catch (Exception e) {
            rejectedCount.incrementAndGet();
            return false;
        }
    }

    /**
     * 强制发布（会阻塞直到可写）——谨慎使用，避免在 Netty IO 线程调用。
     */
    public void publishBlocking(int msgId, BaseHandler<?> task) {
        long sequence = ringBuffer.next();
        try {
            PlayerEvent evt = ringBuffer.get(sequence);
            evt.set(msgId, task, System.nanoTime());
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public int bufferSize() {
        return bufferSize;
    }

    /**
     * 估算 pending 数量：cursor - consumer sequence
     */
    public long getPendingCount() {
        long cursor = ringBuffer.getCursor();
        // consumer sequence is last processed
        long pending = cursor - consumerSequence;
        if (pending < 0) return 0;
        if (pending > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return pending;
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    public long getTotalLatencyNanos() {
        return totalLatencyNanos.get();
    }

    public double getAvgLatencyMillis() {
        long pc = processedCount.get();
        if (pc == 0) return 0.0;
        return (totalLatencyNanos.get() / 1_000_000.0) / pc;
    }

    public long getRejectedCount() {
        return rejectedCount.get();
    }

    public void shutdown(long timeoutMs) {
        try {
            disruptor.shutdown(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("PlayerWorker shutdown interrupted", e);
        }
    }
}
