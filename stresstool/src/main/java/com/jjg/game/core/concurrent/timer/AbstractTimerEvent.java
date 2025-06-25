package com.jjg.game.core.concurrent.timer;

/**
 * 计时器抽象类
 *
 * @author 2CL
 */
public abstract class AbstractTimerEvent implements Runnable {
  /** 首次执行延迟时间 单位:ms */
  private long firstDelay;

  /** 循环执行时间周期 单位:ms */
  private long delay;

  /** 循环执行是否采用固定频率 */
  private boolean loopFixed;

  AbstractTimerEvent() {}

  /**
   * 创建一个只执行一次的 定时器事件
   *
   * @param delay 延迟执行的时间 单位 ms
   */
  public AbstractTimerEvent(long delay) {
    this.firstDelay = delay;
    this.loopFixed = false;
    this.delay = 0;
  }

  /**
   * 创建一个循环执行的计时器事件.
   *
   * @param firstDelay 首次执行的延迟时间（单位:ms）
   * @param delay 循环执行的间隔时间（单位:ms）
   * @param loopFixed 循环执行时是否采用固定频率: true表示不管上一次执行所花费的时间为何，下一次执行必然在initialDelay + 2 * delay后进行；
   *     false表示每次执行必然延迟给定的时间(delay)，即下一次执行会等待上一次执行完成后再计算延迟时间.
   */
  public AbstractTimerEvent(long firstDelay, long delay, boolean loopFixed) {
    this.firstDelay = firstDelay;
    this.delay = delay;
    this.loopFixed = loopFixed;
  }

  /** 获取第一次延迟时间 */
  public long getFirstDelay() {
    return firstDelay;
  }

  /** 设置第一次延迟时间 */
  public void setFirstDelay(long firstDelay) {
    this.firstDelay = firstDelay;
  }

  /** 获得周期执行时间间隔 */
  public long getDelay() {
    return delay;
  }

  /** 设置周期执行时间间隔 */
  public void setDelay(long delay) {
    this.delay = delay;
  }

  /** 是否是循环执行 */
  public boolean isLoopFixed() {
    return loopFixed;
  }

  /** 设置循环执行 */
  public void setLoopFixed(boolean loopFixed) {
    this.loopFixed = loopFixed;
  }
}
