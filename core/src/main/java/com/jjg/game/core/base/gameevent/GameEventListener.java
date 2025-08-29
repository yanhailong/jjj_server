package com.jjg.game.core.base.gameevent;

import java.util.List;

/**
 * 游戏事件监听
 *
 * @author 2CL
 */
public interface GameEventListener {

    /**
     * 处理事件
     *
     * @param gameEvent 事件
     * @param <T>       T
     */
    <T extends GameEvent> void handleEvent(T gameEvent);

    /**
     * 需要监听的事件类型
     *
     * @return 事件类型列表
     */
    List<EGameEventType> needMonitorEvents();
}
