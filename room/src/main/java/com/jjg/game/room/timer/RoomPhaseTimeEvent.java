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

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerEvent<T> event, R room) {
        super(event, room);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count, int initTime, boolean absolute) {
        super(listener, room, parameter, intervalTime, count, initTime, absolute);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, long time, T parameter) {
        super(listener, room, time, parameter);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count, int initTime) {
        super(listener, room, parameter, intervalTime, count, initTime);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count, boolean absolute) {
        super(listener, room, parameter, intervalTime, count, absolute);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , int count) {
        super(listener, room, parameter, intervalTime, count);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime) {
        super(listener, room, parameter, intervalTime);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter, int intervalTime
        , boolean absolute) {
        super(listener, room, parameter, intervalTime, absolute);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, int initTime, T parameter) {
        super(listener, room, initTime, parameter);
        this.eGamePhase = eGamePhase;
    }

    public RoomPhaseTimeEvent(EGamePhase eGamePhase, TimerListener<T> listener, R room, T parameter) {
        super(listener, room, parameter);
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
}
