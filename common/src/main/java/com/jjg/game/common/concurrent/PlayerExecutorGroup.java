package com.jjg.game.common.concurrent;

import com.google.common.hash.Hashing;
import com.jjg.game.common.protostuff.PFSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PlayerExecutorGroup
 * - slotCount 个单线程 worker，每个 worker 有独立有界队列
 * - playerId -> slot 映射：默认使用 (playerId & Long.MAX_VALUE) % slotCount
 * - 提供 submit/execute 方法
 */
public class PlayerExecutorGroup {

    private static final Logger log = LoggerFactory.getLogger(PlayerExecutorGroup.class);

    private final int slotCount;
    private final PlayerWorker[] workers;
    private final AtomicInteger roundRobin = new AtomicInteger();

    /**
     * 构造
     *
     * @param queueSize   每个 worker 的队列大小
     * @param threadNamePrefix 线程名前缀
     */
    public PlayerExecutorGroup(int queueSize, String threadNamePrefix) {
        int cores = Runtime.getRuntime().availableProcessors();
        this.slotCount = cores * 2;
        this.workers = new PlayerWorker[slotCount];

        for (int i = 0; i < slotCount; i++) {
            String name = threadNamePrefix + "-" + i;
            workers[i] = new PlayerWorker(name, queueSize);
        }
    }

    /**
     * 计算需要放入的线程ID,当前暂时通过PFSession的workId进行绑定,后续如果需要平衡负载,可通过processors中的
     * processor对象动态计算
     */
    @SuppressWarnings("UnstableApiUsage")
    private int calcSlot(long workId) {
        //高质量的均匀分布的id
        int hash = Hashing.murmur3_32().hashLong(workId).asInt();
        return (hash & 0x7fffffff) % slotCount;
    }


    /**
     * 提交 Runnable（异步）
     */
    public void execute(long playerId, Runnable task) {
        int idx = calcSlot(playerId);
        workers[idx].execute(task);
    }

    /**
     * 提交 BaseHandler（你项目通用接口）
     */
    public void submitHandler(PFSession session, BaseHandler<?> handler) {
        if (session.getThreadId() == 0) {
            session.setThreadId(calcSlot(session.getWorkId()));
        }
        execute(session.getWorkId(), () -> {
            try {
                long currentTimeMillis = System.currentTimeMillis();
                handler.action();
                long time = System.currentTimeMillis() - currentTimeMillis;
                if (time > 100) {
                    log.error("消息执行超时 msgId:{}", handler.getHandlerParam());
                }
            } catch (Throwable t) {
                log.error("player handler error, workId={}, handler={}", session.getWorkId(), handler.getClass().getName(), t);
            }
        });
    }

    /**
     * 提交 Callable 并返回 Future（用于需要返回值场景）
     */
    public <T> Future<T> submit(long workId, Callable<T> callable) {
        int idx = calcSlot(workId);
        return workers[idx].submit(callable);
    }

    /**
     * 随机分配（用于无玩家ID的任务）
     */
    public void executeAny(Runnable task) {
        int i = Math.abs(roundRobin.getAndIncrement() % slotCount);
        workers[i].execute(task);
    }

    /**
     * 查询 slot 队列长度（监控）
     */
    public int getQueueSizeBySlot(int slot) {
        return workers[slot].getQueueSize();
    }

    public int getSlotCount() {
        return slotCount;
    }

    /**
     * 优雅停机
     */
    public void shutdownGracefully(long awaitMillis) {
        for (PlayerWorker w : workers) {
            w.shutdown();
        }
        long deadline = System.currentTimeMillis() + awaitMillis;
        for (PlayerWorker w : workers) {
            long left = Math.max(0, deadline - System.currentTimeMillis());
            try {
                w.awaitTermination(left, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------
    // 内部 Worker 实现（单线程 + 有界队列 + CallerRunsPolicy）
    // ------------------------------
    static class PlayerWorker {

        private final ThreadPoolExecutor executor;
        private final String name;

        PlayerWorker(String name, int queueSize) {
            this.name = name;
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(Math.max(1, queueSize));
            this.executor = new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    queue,
                    new NamedThreadFactory(name),
                    new ThreadPoolExecutor.DiscardPolicy() // backpressure: 在调用线程执行
            );
            // 预启动线程
            this.executor.prestartAllCoreThreads();
        }

        void execute(Runnable r) {
            try {
                if (executor.isShutdown()) {
                    throw new RejectedExecutionException("worker shutdown: " + name);
                }
                executor.execute(r);
            } catch (RejectedExecutionException rex) {
                // 在极端情况下也可做降级处理：记录、告警、落盘等
                log.warn("Rejected task on worker {} - fallback run in caller", name);
                // fallback: run in caller
                try {
                    r.run();
                } catch (Throwable t) {
                    log.error("fallback run error", t);
                }
            }
        }

        <T> Future<T> submit(Callable<T> c) {
            if (executor.isShutdown()) {
                throw new RejectedExecutionException("worker shutdown: " + name);
            }
            return executor.submit(c);
        }

        int getQueueSize() {
            return executor.getQueue().size();
        }

        void shutdown() {
            executor.shutdown();
        }

        boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executor.awaitTermination(timeout, unit);
        }
    }

    // 简单线程工厂（带名字）
    static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger idx = new AtomicInteger(0);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + idx.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }

}
