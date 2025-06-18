package com.jjg.game.common.timer;

/**
 * 定时事件监听器
 *
 * @since 1.0
 */
public interface TimerListener {
    /**
     * 定时事件的监听方法
     */
    void onTimer(TimerEvent e);
}
