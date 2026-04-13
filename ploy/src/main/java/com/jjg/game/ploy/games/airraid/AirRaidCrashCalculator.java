package com.jjg.game.ploy.games.airraid;

import com.jjg.game.common.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 空袭坠毁倍率计算器
 * <p>
 * 倍率增长公式: Mt = (1 + r)^t  (r为万分比增长率, t为秒数)
 * 坠毁概率公式: Pt = p0 + (t * k) (p0为初始坠毁率, k为风险增量, 均为万分比)
 * <p>
 * 所有倍率使用万分比表示: 10000 = 1.00x, 15000 = 1.50x, 100000 = 10.00x
 * </p>
 *
 * @author 11
 * @date 2026/3/27
 */
public final class AirRaidCrashCalculator {
    /**
     * 使用坠毁概率公式预生成本局坠毁时间(秒)
     * <p>
     * 每秒判定一次: 生成随机数 [1, 10000]，若 <= Pt 则坠毁。
     * Pt = p0 + (t * k)，随时间线性增长，保证游戏最终会结束。
     * </p>
     *
     * @param p0 初始坠毁概率(万分比, 如300表示3%)
     * @param k  每秒风险增量(万分比, 如60表示0.6%/秒)
     * @return 坠毁时间(秒), 最小为0
     */
    public static int generateCrashTime(int p0, int k) {
        for (int t = 0; t < 10000; t++) {
            int pt = p0 + t * k;
            // 概率 >= 100% 必然坠毁
            if (pt >= 10000) {
                return Math.max(t, 0);
            }
            int roll = RandomUtils.nextIntInclude(1, 10000);
            if (roll <= pt) {
                return Math.max(t, 0);
            }
        }
        // 理论上不会到这里(概率会增长到100%)，兜底返回
        return 0;
    }

    /**
     * 根据坠毁时间和增长率计算坠毁倍率
     * <p>
     * 公式: Mt = (1 + r)^t
     * </p>
     *
     * @param crashTimeSec 坠毁时间(秒)
     * @param growthRate   倍数增长率(万分比, 如1200表示12%)
     * @return 坠毁倍率(万分比), 最低10000(1.00x)
     */
    public static int calculateCrashMultiplier(int crashTimeSec, int growthRate) {
        double r = growthRate / 10000.0;
        double multiplier = Math.pow(1 + r, crashTimeSec);
        return Math.max((int) (multiplier * 10000), 10000);
    }

    /**
     * 根据飞行经过时间计算当前实时倍率
     * <p>
     * 公式: Mt = (1 + r)^t，其中 t = elapsedMs / 1000.0
     * </p>
     *
     * @param elapsedMs  飞行经过的毫秒数
     * @param growthRate 倍数增长率(万分比, 如1200表示12%)
     * @return 当前倍率(万分比)
     */
    public static int calculateCurrentMultiplier(long elapsedMs, int growthRate) {
        double r = growthRate / 10000.0;
        double t = elapsedMs / 1000.0;
        double multiplier = Math.pow(1 + r, t);
        return (int) (multiplier * 10000);
    }

    /**
     * 根据坠毁倍率和增长速率计算飞行持续时间
     * <p>
     * 由 Mt = (1+r)^t 反推: t = ln(Mt) / ln(1+r)
     * </p>
     *
     * @param crashMultiplier 坠毁倍率(万分比)
     * @param growthRate      倍数增长率(万分比)
     * @return 飞行持续时间(毫秒)
     */
    public static long calculateFlyDuration(int crashMultiplier, int growthRate) {
        double r = growthRate / 10000.0;
        if (r <= 0) {
            r = 0.12;
        }
        double targetMultiplier = crashMultiplier / 10000.0;
        double seconds = Math.log(targetMultiplier) / Math.log(1 + r);
        return (long) (seconds * 1000);
    }
}
