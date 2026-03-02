package com.jjg.game.core.base.gameevent;

import java.time.LocalDate;

/**
 * 整点事件
 *
 * @author lm
 * @date 2025/9/25 15:02
 */
public class ClockEvent extends GameEvent {
    //触发的整点时间
    private final int hour;
    // 业务日期（GM 测试可指定，null 表示使用系统当前日期）
    private final LocalDate businessDate;

    public ClockEvent(EGameEventType gameEventType, int hour) {
        this(gameEventType, hour, null);
    }

    public ClockEvent(EGameEventType gameEventType, int hour, LocalDate businessDate) {
        super(gameEventType);
        this.hour = hour;
        this.businessDate = businessDate;
    }

    public int getHour() {
        return hour;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }
}
