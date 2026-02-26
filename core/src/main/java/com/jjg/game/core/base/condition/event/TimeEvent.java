package com.jjg.game.core.base.condition.event;

/**
 * @author lm
 * @date 2026/2/25 15:46
 */
public class TimeEvent {
    private long startTime;
    private long endTime;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
