package com.jjg.game.room.timer;

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
    // 房间事件类型
    private final RoomEventType roomEventType;

    public RoomTimerEvent(TimerEvent<T> event, R room, RoomEventType roomEventType) {
        super(event.getTimerListener(), event.getParameter());
        this.intervalTime = event.getIntervalTime();
        this.count = event.getCount();
        this.initTime = event.getInitTime();
        this.nextTime = event.getNextTime();
        this.absolute = event.isAbsolute();
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count, int initTime,
                          boolean absolute, RoomEventType roomEventType) {
        super(listener, parameter, intervalTime, count, initTime, absolute);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, long time, T parameter, RoomEventType roomEventType) {
        super(listener, time, parameter);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count, int initTime,
                          RoomEventType roomEventType) {
        super(listener, parameter, intervalTime, count, initTime);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count,
                          boolean absolute, RoomEventType roomEventType) {
        super(listener, parameter, intervalTime, count, absolute);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, int count,
                          RoomEventType roomEventType) {
        super(listener, parameter, intervalTime, count);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime,
                          RoomEventType roomEventType) {
        super(listener, parameter, intervalTime);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, int intervalTime, boolean absolute,
                          RoomEventType roomEventType) {
        super(listener, parameter, intervalTime, absolute);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, int initTime, T parameter, RoomEventType roomEventType) {
        super(listener, initTime, parameter);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public RoomTimerEvent(TimerListener<T> listener, R room, T parameter, RoomEventType roomEventType) {
        super(listener, parameter);
        this.room = room;
        this.roomEventType = roomEventType;
    }

    public long getRoomId() {
        return this.room.getId();
    }

    @Override
    public String toString() {
        return super.toString() + "事件类型：" + roomEventType.getEventTypeName() + " 房间信息：" + room.logStr();
    }
}
