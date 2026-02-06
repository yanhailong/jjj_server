package com.jjg.game.common.utils;

import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 封装的 HashedWheelTimer 工具类
 * 支持一次性任务 & 周期性任务
 * 需要在其他线程执行逻辑任务
 *
 * @author lm
 * @date 2025/9/29 14:29
 */
public class WheelTimerUtil {

    private static final HashedWheelTimer TIMER;
    private static final Logger log = LoggerFactory.getLogger(WheelTimerUtil.class);

    /**
     * 周期性任务（固定间隔）
     *
     * @param task         执行内容
     * @param initialDelay 首次延迟
     * @param period       间隔时间
     * @param unit         时间单位
     * @return 可用 Timeout 取消任务
     */
    public static Timeout scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        final RecurringTimeout[] holder = new RecurringTimeout[1];
        // 包装一个递归任务
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                RecurringTimeout recurringTimeout = holder[0];
                if (timeout.isCancelled() || recurringTimeout.isCancelled()) {
                    return;
                }
                try {
                    task.run();

                } catch (Exception e) {
                    log.error("轮询定时器异常", e);
                } finally {
                    // 再次调度自身
                    if (!recurringTimeout.isCancelled()) {
                        Timeout nextTimeout = TIMER.newTimeout(this, period, unit);
                        recurringTimeout.setDelegate(nextTimeout);
                    }
                }
            }
        };
        RecurringTimeout recurringTimeout = new RecurringTimeout(timerTask);
        holder[0] = recurringTimeout;
        Timeout firstTimeout = TIMER.newTimeout(timerTask, initialDelay, unit);
        recurringTimeout.setDelegate(firstTimeout);
        return recurringTimeout;
    }

    static {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "wheel-timer-" + index.getAndIncrement());
            }
        };

        // 推荐参数：100ms 精度，1024 槽
        TIMER = new HashedWheelTimer(
                threadFactory,
                100, TimeUnit.MILLISECONDS,
                1024,
                true,
                1_000_000
        );
    }

    /**
     * 一次性延迟任务
     */
    public static Timeout schedule(Runnable task, long delay, TimeUnit unit) {
        return TIMER.newTimeout(timeout -> task.run(), delay, unit);
    }

    /**
     * 周期性任务（固定间隔）
     *
     * @param task         执行内容
     * @param initialDelay 首次延迟
     * @param periodMax    最大间隔时间
     * @param periodMin    最小间隔时间
     * @param unit         时间单位
     * @return 可用 Timeout 取消任务
     */
    public static Timeout scheduleAtRangeRate(Runnable task, long initialDelay, long periodMin, long periodMax, TimeUnit unit) {
        final RecurringTimeout[] holder = new RecurringTimeout[1];
        // 包装一个递归任务
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                RecurringTimeout recurringTimeout = holder[0];
                if (timeout.isCancelled() || recurringTimeout.isCancelled()) {
                    return;
                }
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("轮询定时器异常", e);
                } finally {
                    // 再次调度自身
                    if (!recurringTimeout.isCancelled()) {
                        Timeout nextTimeout = TIMER.newTimeout(this, RandomUtil.randomLong(periodMin, periodMax), unit);
                        recurringTimeout.setDelegate(nextTimeout);
                    }
                }
            }
        };
        RecurringTimeout recurringTimeout = new RecurringTimeout(timerTask);
        holder[0] = recurringTimeout;
        Timeout firstTimeout = TIMER.newTimeout(timerTask, initialDelay, unit);
        recurringTimeout.setDelegate(firstTimeout);
        return recurringTimeout;
    }

    /**
     * 周期性任务（固定次数，随机时间）
     *
     * @param task         执行内容
     * @param initialDelay 首次延迟
     * @param random       权重随机时间
     * @param unit         时间单位
     * @return 可用 Timeout 取消任务
     */
    public static Timeout scheduleAtFixedCount(Runnable task, AtomicInteger times, long initialDelay, WeightRandom<Integer> random, TimeUnit unit) {
        final RecurringTimeout[] holder = new RecurringTimeout[1];
        // 包装一个递归任务
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                RecurringTimeout recurringTimeout = holder[0];
                if (timeout.isCancelled() || recurringTimeout.isCancelled()) {
                    return;
                }
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("轮询定时器异常", e);
                } finally {
                    int get = times.decrementAndGet();
                    if (get > 0 && !recurringTimeout.isCancelled()) {
                        // 再次调度自身
                        Timeout nextTimeout = TIMER.newTimeout(this, random.next(), unit);
                        recurringTimeout.setDelegate(nextTimeout);
                    }
                }
            }
        };
        RecurringTimeout recurringTimeout = new RecurringTimeout(timerTask);
        holder[0] = recurringTimeout;
        Timeout firstTimeout = TIMER.newTimeout(timerTask, initialDelay, unit);
        recurringTimeout.setDelegate(firstTimeout);
        return recurringTimeout;
    }

    /**
     * 周期任务的可取消句柄。通过代理当前实际Timeout，保证递归调度下 cancel() 依然有效。
     */
    private static final class RecurringTimeout implements Timeout {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final TimerTask timerTask;
        private volatile Timeout delegate;

        private RecurringTimeout(TimerTask timerTask) {
            this.timerTask = timerTask;
        }

        private void setDelegate(Timeout delegate) {
            this.delegate = delegate;
        }

        @Override
        public Timer timer() {
            Timeout timeout = delegate;
            return timeout == null ? TIMER : timeout.timer();
        }

        @Override
        public TimerTask task() {
            return timerTask;
        }

        @Override
        public boolean isExpired() {
            Timeout timeout = delegate;
            return timeout != null && timeout.isExpired();
        }

        @Override
        public boolean isCancelled() {
            if (cancelled.get()) {
                return true;
            }
            Timeout timeout = delegate;
            return timeout != null && timeout.isCancelled();
        }

        @Override
        public boolean cancel() {
            cancelled.set(true);
            Timeout timeout = delegate;
            return timeout == null || timeout.cancel();
        }
    }

    /**
     * 关闭定时器（应用退出时调用）
     */
    public static void stop() {
        TIMER.stop();
    }

}
