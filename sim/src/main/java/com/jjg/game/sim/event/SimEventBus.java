package com.jjg.game.sim.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * sim 模块事件总线 (同步派发)
 * <p>
 * 同步派发的好处: 派发线程 = 触发线程 = 玩家线程 (PlayerExecutorGroupDisruptor 已做隔离),
 * 监听器内部可直接读写 ctx, 无需加锁。
 * <p>
 * 缺点: 监听器异常会传播; 已用 try/catch 隔离, 不影响其它监听器与触发者。
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

    @PostConstruct
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
