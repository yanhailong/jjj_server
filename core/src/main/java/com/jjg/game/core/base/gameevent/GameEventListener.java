package com.jjg.game.core.base.gameevent;

import java.util.List;
import java.util.Map;

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
     * 需要监听的事件类型, 根据实际需要监听的类型写入，通过配置表配置或者手动配置，需尽量避免写入无关事件类型
     *
     * @return 事件类型列表
     */
    List<EGameEventType> needMonitorEvents();

    /**
     * 获取事件子类型
     * @return 事件子类型
     */
    default Map<EGameEventType, Object> getSubTypeMap() {
        return Map.of();
    }
}
