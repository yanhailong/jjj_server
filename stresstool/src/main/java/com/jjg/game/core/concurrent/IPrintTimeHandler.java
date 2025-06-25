package com.jjg.game.core.concurrent;

/**
 * 打印接口
 *
 * @author 2CL
 */
public interface IPrintTimeHandler {

  /**
   * handler等待时间
   *
   * @param handler
   * @param logDoTime
   * @param waitTime
   * @param callWay
   */
  void printWaitTime(BaseHandler handler, long logDoTime, long waitTime, String callWay);

  /**
   * handler执行时间
   *
   * @param handler
   * @param doTime
   * @param info
   */
  void printDoTime(BaseHandler handler, long doTime, String info);

  /**
   * handler队列大小
   *
   * @param handler
   * @param size 当前队列大小
   */
  void printQueueSize(BaseHandler handler, int size);
}
