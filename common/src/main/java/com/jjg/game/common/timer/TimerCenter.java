package com.jjg.game.common.timer;

/**
 * 系统公共 定时器 or 线程池
 *
 * @author nobody
 * @since 1.0
 */
public class TimerCenter extends BaseTimerCenter<TimerEvent<?>> {

    public TimerCenter(String timerName) {
        super(timerName);
    }

    public TimerCenter(String timerName, int corePoolSize, int maximumPoolSize, int queueSize) {
        super(timerName, corePoolSize, maximumPoolSize, queueSize);
    }
}
