package com.jjg.game.sim.service;

import com.jjg.game.sim.data.SimPlayerContext;

/**
 * 玩家级定时回调
 * <p>
 * 实现该接口并标注为 Spring Bean 后, SimManager 的定时器会自动遍历调用。
 * 每个回调都在玩家自己的执行线程中执行 (PlayerExecutorGroupDisruptor 调度),
 * 因此实现内部可以放心读写 ctx, 无需自行加锁。
 * <p>
 * 典型用途:
 * <pre>
 * - 游客生成 (SimGuestService)
 * - 建筑 CD 完成检测 / 产出累积 (后续 BuildingService)
 * - 离线收益结算 (后续 OfflineRewardService)
 * </pre>
 *
 * @author 11
 * @date 2026/5/26
 */
public interface SimPlayerTickHandler {

    /**
     * 每次定时器触发时被调用 (调度间隔由 SimManager 决定, 当前为 2s)
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
