package com.jjg.game.room.timer;

import com.jjg.game.common.concurrent.BaseProcessor;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.data.Room;

/**
 * 房间timer事件, interval的时间不能低于TimerCenter的RunTime时间，否则不会按预期时间执行
 *
 * @author 2CL
 */
public class RoomTimerEvent<T extends IProcessorHandler, R extends Room> extends TimerEvent<T> {

    private final R room;

    public RoomTimerEvent(TimerEvent<T> event, R room) {
        super(event.getTimerListener(), event.getParameter());
        this.intervalTime = event.getIntervalTime();
        this.count = event.getCount();
        this.initTime = event.getInitTime();
        this.absolute = event.isAbsolute();
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count, int initTime,
                          boolean absolute) {
        super(listener, parameter, intervalTime, count, initTime, absolute);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, long time, T parameter) {
        super(listener, time, parameter);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count, int initTime) {
        super(listener, parameter, intervalTime, count, initTime);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count,
                          boolean absolute) {
        super(listener, parameter, intervalTime, count, absolute);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count) {
        super(listener, parameter, intervalTime, count);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime) {
        super(listener, parameter, intervalTime);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, boolean absolute) {
        super(listener, parameter, intervalTime, absolute);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, int initTime, T parameter) {
        super(listener, initTime, parameter);
        this.room = room;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter) {
        super(listener, parameter);
        this.room = room;
    }

    public long getRoomId() {
        return this.room.getId();
    }
}
