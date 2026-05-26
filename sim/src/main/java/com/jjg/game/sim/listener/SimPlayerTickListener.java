package com.jjg.game.sim.listener;

import com.jjg.game.sim.data.SimPlayerContext;

/**
 * 玩家级定时回调
 *
 * @author 11
 * @date 2026/5/26
 */
public interface SimPlayerTickListener {

    /**
     * 每次定时器触发时被调用 (调度间隔由 SimManager 决定)
     *
     * @param ctx 玩家上下文
     * @param now 当前时间 (ms), 由调度器统一传入避免逐个回调取时钟
     */
    void onTick(SimPlayerContext ctx, long now);

    /**
     * 执行顺序; 数值越小越先执行。默认 100。
     */
    default int order() {
        return 100;
    }
}
