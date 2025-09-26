package com.jjg.game.core.base.gameevent;

/**
 * 整点事件
 *
 * @author lm
 * @date 2025/9/25 15:02
 */
public class ClockEvent extends GameEvent {
    //触发的整点时间
    private final int hour;
    public ClockEvent(EGameEventType gameEventType, int hour) {
        super(gameEventType);
        this.hour = hour;
    }

    public int getHour() {
        return hour;
    }
}
