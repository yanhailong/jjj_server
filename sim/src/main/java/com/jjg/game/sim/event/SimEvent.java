package com.jjg.game.sim.event;

import com.jjg.game.sim.data.SimPlayerContext;

/**
 * sim 模块事件基类
 * <p>
 * 所有事件都携带触发它的 SimPlayerContext, 监听器可直接拿到玩家上下文操作。
 *
 * @author 11
 * @date 2026/5/26
 */
public abstract class SimEvent {

    private final SimPlayerContext ctx;
    private final long timestamp;

    protected SimEvent(SimPlayerContext ctx) {
        this.ctx = ctx;
        this.timestamp = System.currentTimeMillis();
    }

    public SimPlayerContext ctx() {
        return ctx;
    }

    public long timestamp() {
        return timestamp;
    }
}
