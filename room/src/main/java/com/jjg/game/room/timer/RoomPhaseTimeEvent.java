package com.jjg.game.room.timer;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.data.Room;
import com.jjg.game.room.constant.EGamePhase;

/**
 * 房间在具体某个阶段添加的阶段时间任务，防止在阶段切换后还在添加上一个阶段的定时任务
 *
 * @author 2CL
 */
public class RoomPhaseTimeEvent<T extends IProcessorHandler, R extends Room> extends RoomTimerEvent<T, R> {

    private final EGamePhase eGamePhase;
    /**
     * 是否可以跨阶段执行
     */
    private boolean canAcrossPhaseExec = false;

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerEvent<T> event, R room, RoomEventType roomEventType) {
        super(event, room, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count, int initTime, boolean absolute, RoomEventType roomEventType) {
        super(listener, room, parameter, intervalTime, count, initTime, absolute, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, long time, T parameter,
                              RoomEventType roomEventType) {
        super(listener, room, time, parameter, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count, int initTime, RoomEventType roomEventType) {
        super(listener, room, parameter, intervalTime, count, initTime, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count, boolean absolute, RoomEventType roomEventType) {
        super(listener, room, parameter, intervalTime, count, absolute, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count, RoomEventType roomEventType) {
        super(listener, room, parameter, intervalTime, count, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , RoomEventType roomEventType) {
        super(listener, room, parameter, intervalTime, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , boolean absolute, RoomEventType roomEventType) {
        super(listener, room, parameter, intervalTime, absolute, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, int initTime, T parameter,
                              RoomEventType roomEventType) {
        super(listener, room, initTime, parameter, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter,
                              RoomEventType roomEventType) {
        super(listener, room, parameter, roomEventType);
        this.eGamePhase = eGamePhase;
    }

    public EGamePhase geteGamePhase() {
        return eGamePhase;
    }

    public boolean isCanAcrossPhaseExec() {
        return canAcrossPhaseExec;
    }

    public void setCanAcrossPhaseExec(boolean canAcrossPhaseExec) {
        this.canAcrossPhaseExec = canAcrossPhaseExec;
    }

    @Override
    public String toString() {
        return super.toString() + eGamePhase.getPhaseName();
    }
}
