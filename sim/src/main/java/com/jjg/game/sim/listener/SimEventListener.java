package com.jjg.game.sim.listener;

import com.jjg.game.sim.data.SimEvent;

/**
 * sim 模块事件监听器
 *
 * @author 11
 * @date 2026/5/26
 */
public interface SimEventListener<E extends SimEvent> {

    /**
     * 监听的事件类型 (用于 SimEventBus 路由)
     */
    Class<E> eventType();

    /**
     * 事件处理
     */
    void onEvent(E event);

    /**
     * 执行顺序; 数值越小越先执行。默认 100。
     */
    default int order() {
        return 100;
    }
}
