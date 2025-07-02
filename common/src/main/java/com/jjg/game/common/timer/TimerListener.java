package com.jjg.game.common.timer;

/**
 * 定时事件监听器
 *
 * @author nobody
 * @since 1.0
 */
public interface TimerListener<T> {
    /**
     * 定时事件的监听方法
     */
    void onTimer(TimerEvent<T> e);
}
