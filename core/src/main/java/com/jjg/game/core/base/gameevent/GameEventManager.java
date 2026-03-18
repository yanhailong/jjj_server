package com.jjg.game.core.base.gameevent;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏事件管理器
 *
 * @author 2CL
 */
@Component
public class GameEventManager {

    private static final Logger log = LoggerFactory.getLogger(GameEventManager.class);
    /**
     * 事件类型map
     */
    private final Map<EGameEventType, Set<GameEventListener>> eventListMap = new ConcurrentHashMap<>();
    private final ClusterSystem clusterSystem;

    public GameEventManager(ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    /**
     * 初始化事件监听器
     */
    public void initEventListener() {
        Map<String, GameEventListener> gameEventListenerMap =
                CommonUtil.getContext().getBeansOfType(GameEventListener.class);
        for (Map.Entry<String, GameEventListener> entry : gameEventListenerMap.entrySet()) {
            GameEventListener gameEventListener = entry.getValue();
            // 注册事件监听器
            registerEventListener(gameEventListener);
        }
    }

    /**
     * 注册事件监听，用于GameEventManager初始化扫描不到的EventListener
     */
    public void registerEventListener(GameEventListener gameEventListener) {
        // 单个监听器可以监听多个事件
        List<EGameEventType> gameEventTypes = gameEventListener.needMonitorEvents();
        for (EGameEventType gameEventType : gameEventTypes) {
            // 注册监听器
            eventListMap.computeIfAbsent(gameEventType, k -> new HashSet<>()).add(gameEventListener);
        }
        log.info("注册事件监听器：{} 成功", gameEventListener.getClass().getName());
    }


    /**
     * 触发事件
     *
     * @param gameEvent 游戏事件
     */
    public <T extends GameEvent> void triggerEvent(T gameEvent) {
        EGameEventType gameEventType = gameEvent.getGameEventType();
        Set<GameEventListener> eventListeners = eventListMap.get(gameEventType);
        if (eventListeners == null || eventListeners.isEmpty()) {
            return;
        }
        if (gameEvent instanceof PlayerEvent event) {
            PFSession session = clusterSystem.getSession(event.getPlayer().getId());
            if (session != null) {
                boolean published = PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        // 处理事件
                        for (GameEventListener eventListener : eventListeners) {
                            //避免其中某个服务在处理事件耗时太久导致事件触发出现延迟
                            try {
                                log.debug("listener: {} 响应事件：{}", eventListener.getClass().getName(), gameEventType);
                                eventListener.handleEvent(gameEvent);
                            } catch (Exception exception) {
                                log.error("listener: {} 触发事件：{} 时出现异常：{}",
                                        eventListener.getClass().getName(), gameEventType, exception.getMessage(), exception);
                            }
                        }
                    }
                }.setHandlerParamWithSelf("event %s".formatted(gameEventType.name())));
                if (published) {
                    return;
                }
                log.error("玩家事件分发失败，降级异步处理 playerId:{} gameEventType:{}", event.getPlayer().getId(), gameEventType);
            } else {
                log.error("玩家事件 session为null playerId:{} gameEventType:{}", event.getPlayer().getId(), gameEventType);
            }
        }
        // 处理事件
        for (GameEventListener eventListener : eventListeners) {
            //避免其中某个服务在处理事件耗时太久导致事件触发出现延迟
            Thread.ofVirtual().start(() -> {
                try {
//                    log.debug("listener: {} 响应事件：{}", eventListener.getClass().getName(), gameEventType);
                    eventListener.handleEvent(gameEvent);
                } catch (Exception exception) {
                    log.error("listener: {} 触发事件：{} 时出现异常：{}",
                            eventListener.getClass().getName(), gameEventType, exception.getMessage(), exception);
                }
            });
        }
    }


    /**
     * 触发事件
     *
     * @param gameEvent 游戏事件
     */
    public <T extends GameEvent> void syncTriggerEvent(T gameEvent) {
        EGameEventType gameEventType = gameEvent.getGameEventType();
        Set<GameEventListener> eventListeners = eventListMap.get(gameEventType);
        if (eventListeners == null || eventListeners.isEmpty()) {
            return;
        }
        List<GameEventListener> gameEventListeners = new ArrayList<>(eventListeners);
        gameEventListeners.sort((o1, o2) -> {
            Map<EGameEventType, Integer> order1 = o1.evetOrder();
            Map<EGameEventType, Integer> order2 = o2.evetOrder();
            Integer o1Order = order1.getOrDefault(gameEventType, 0);
            Integer o2Order = order2.getOrDefault(gameEventType, 0);
            return Integer.compare(o1Order, o2Order);
        });
        // 处理事件
        for (GameEventListener eventListener : gameEventListeners) {
            //避免其中某个服务在处理事件耗时太久导致事件触发出现延迟
            try {
                eventListener.handleEvent(gameEvent);
            } catch (Exception exception) {
                log.error("listener: {} 触发事件：{} 时出现异常：{}", eventListener.getClass().getName(), gameEventType, exception.getMessage(), exception);
            }
            if (eventListener.stopEventPropagation(gameEventType)) {
                break;
            }
        }
    }

}
