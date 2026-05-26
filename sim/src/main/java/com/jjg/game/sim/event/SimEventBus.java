package com.jjg.game.sim.event;

import com.jjg.game.sim.listener.SimEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * sim 模块事件总线 (同步派发)
 *
 * @author 11
 * @date 2026/5/26
 */
@Component
public class SimEventBus {
    private static final Logger log = LoggerFactory.getLogger(SimEventBus.class);

    @SuppressWarnings("rawtypes")
    @Autowired(required = false)
    private List<SimEventListener> rawListeners;

    /** eventType → 监听器列表 (按 order 升序) */
    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends SimEvent>, List<SimEventListener>> listenerMap = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void init() {
        if (rawListeners == null || rawListeners.isEmpty()) {
            return;
        }
        for (SimEventListener listener : rawListeners) {
            Class<? extends SimEvent> type = listener.eventType();
            listenerMap.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
        }
        for (List<SimEventListener> list : listenerMap.values()) {
            list.sort(Comparator.comparingInt(SimEventListener::order));
        }
        log.info("SimEventBus 初始化完成, 注册事件类型 {} 个", listenerMap.size());
    }

    /**
     * 派发事件 (同步)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends SimEvent> void publish(E event) {
        if (event == null) {
            return;
        }
        List<SimEventListener> listeners = listenerMap.get(event.getClass());
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (SimEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("事件监听器异常 listener={},event={}", listener.getClass().getSimpleName(), event.getClass().getSimpleName(), e);
            }
        }
    }
}
