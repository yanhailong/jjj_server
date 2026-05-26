package com.jjg.game.sim.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作速率限制工具
 * <p>
 * 两种用法:
 * <pre>
 * 1) 静态判定 (调用方自己维护 lastTime):
 *    if (!SimRateLimiter.tryAcquire(building.getLastUpgradeOpTime(), 500)) return TOO_FAST;
 *    building.setLastUpgradeOpTime(System.currentTimeMillis());
 *
 * 2) 实例 (基于 key 维度自动记录):
 *    private final SimRateLimiter limiter = new SimRateLimiter(500);
 *    if (!limiter.tryAcquire(playerId + ":" + buildingId)) return TOO_FAST;
 * </pre>
 *
 * @author 11
 * @date 2026/5/26
 */
public class SimRateLimiter {

    /**
     * 静态判定: 距离上次操作是否已超过 intervalMs
     *
     * @param lastTimeMs 上次操作的时间戳; 0 表示从未操作过
     * @param intervalMs 最小间隔(毫秒)
     * @return true=允许; false=过快, 拒绝
     */
    public static boolean tryAcquire(long lastTimeMs, long intervalMs) {
        if (lastTimeMs <= 0) {
            return true;
        }
        return (System.currentTimeMillis() - lastTimeMs) >= intervalMs;
    }

    // ---------------------------------------------------------------------
    // 实例: 基于 key 的限流
    // ---------------------------------------------------------------------

    private final long intervalMs;
    private final Map<String, Long> lastTimeMap = new HashMap<>();

    public SimRateLimiter(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    /**
     * 基于 key 的限流: 同 key 两次操作必须间隔 ≥ intervalMs
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Long last = lastTimeMap.get(key);
        if (last != null && (now - last) < intervalMs) {
            return false;
        }
        lastTimeMap.put(key, now);
        return true;
    }

    /**
     * 重置某 key 的限流状态 (玩家退出 / 数据清理时调用)
     */
    public void reset(String key) {
        lastTimeMap.remove(key);
    }
}
