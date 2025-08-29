package com.jjg.game.core.base.gameevent;

/**
 * 游戏事件/系统事件
 *
 * @author 2CL
 */
public class GameEvent {

    /**
     * 游戏事件类型
     */
    private final EGameEventType gameEventType;

    /**
     * 事件产生的变化值，可能是 增量或减少量
     */
    private Object eventChangeValue;

    /**
     * 事件最新值
     */
    private Object newlyValue;


    public GameEvent(EGameEventType gameEventType) {
        this.gameEventType = gameEventType;
    }

    public GameEvent(EGameEventType gameEventType, Object eventChangeValue, Object newlyValue) {
        this.gameEventType = gameEventType;
        this.eventChangeValue = eventChangeValue;
        this.newlyValue = newlyValue;
    }

    public EGameEventType getGameEventType() {
        return gameEventType;
    }

    public Object getEventChangeValue() {
        return eventChangeValue;
    }

    public void setEventChangeValue(Object eventChangeValue) {
        this.eventChangeValue = eventChangeValue;
    }

    public Object getNewlyValue() {
        return newlyValue;
    }

    public void setNewlyValue(Object newlyValue) {
        this.newlyValue = newlyValue;
    }
}
