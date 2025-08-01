package com.jjg.game.common.concurrent;

/**
 * 打印接口
 *
 * @author 2CL
 */
public interface IPrintTimeHandler {

    /**
     * handler等待时间
     */
    void printWaitTime(BaseHandler<?> handler, long logDoTime, long waitTime, String callWay);

    /**
     * handler执行时间
     */
    void printDoTime(BaseHandler<?> handler, long doTime, String info);

    /**
     * handler队列大小
     *
     * @param size 当前队列大小
     */
    void printQueueSize(BaseHandler<?> handler, int size);
}
