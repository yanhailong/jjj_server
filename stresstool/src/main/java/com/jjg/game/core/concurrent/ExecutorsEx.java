package com.jjg.game.core.concurrent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor替代类(阿里P3C)
 *
 * @author 2CL
 */
public class ExecutorsEx {

  /**
   * 创建一个定时器
   *
   * @param threadName
   * @param runnable
   * @param initialDelay
   * @param period
   * @param unit
   */
  public static void newSingleThreadScheduledExecutor(
          String threadName, Runnable runnable, long initialDelay, long period, TimeUnit unit) {
    newSingleThreadScheduledExecutor(threadName, runnable, initialDelay, period, unit, false);
  }

  /**
   * 创建一个定时器
   *
   * @param threadName
   * @param runnable
   * @param initialDelay
   * @param period
   * @param unit
   * @param daemon
   */
  public static void newSingleThreadScheduledExecutor(
          String threadName,
          Runnable runnable,
          long initialDelay,
          long period,
          TimeUnit unit,
          boolean daemon) {

    ScheduledExecutorService executor =
            new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory("threadName", daemon));

    executor.scheduleAtFixedRate(runnable, initialDelay, period, unit);
  }
}
