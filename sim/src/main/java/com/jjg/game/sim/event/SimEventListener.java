package com.jjg.game.sim.event;

/**
 * sim 模块事件监听器
 * <p>
 * 实现该接口并标注为 Spring Bean, SimEventBus 会自动按事件类型路由。
 * <pre>
 * &#064;Service
 * public class GuestPoolRefresher implements SimEventListener&lt;BuildingUnlockedEvent&gt; {
 *     public Class&lt;BuildingUnlockedEvent&gt; eventType() { return BuildingUnlockedEvent.class; }
 *     public void onEvent(BuildingUnlockedEvent e) { ... }
 * }
 * </pre>
 *
 * @param <E> 感兴趣的事件类型
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
