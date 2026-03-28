package com.jjg.game.ploy.games.airraid;

import com.jjg.game.common.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 空袭游戏坠毁倍率计算器（临时实现）
 * <p>
 * 所有倍率使用万分比表示：10000 = 1.00x, 15000 = 1.50x, 100000 = 10.00x
 * </p>
 *
 * @author 11
 * @date 2026/3/27
 */
public final class AirRaidCrashCalculator {

    private static final Logger log = LoggerFactory.getLogger(AirRaidCrashCalculator.class);

    private AirRaidCrashCalculator() {
    }

    /**
     * 生成坠毁倍率（临时简单概率分布）
     * <ul>
     *   <li>50% 概率: 1.00x ~ 2.00x (10000 ~ 20000)</li>
     *   <li>30% 概率: 2.00x ~ 5.00x (20000 ~ 50000)</li>
     *   <li>15% 概率: 5.00x ~ 10.00x (50000 ~ 100000)</li>
     *   <li>5% 概率: 10.00x ~ 100.00x (100000 ~ 1000000)</li>
     * </ul>
     *
     * @return 坠毁倍率（万分比）
     */
    public static int generateCrashMultiplier() {
        // 1~100 的随机数（包含两端）
        int roll = RandomUtils.getRandomNumInt100();
        int crashMultiplier;
        if (roll <= 50) {
            // 50%: [10000, 20000]
            crashMultiplier = RandomUtils.nextIntInclude(10000, 20000);
        } else if (roll <= 80) {
            // 30%: [20000, 50000]
            crashMultiplier = RandomUtils.nextIntInclude(20000, 50000);
        } else if (roll <= 95) {
            // 15%: [50000, 100000]
            crashMultiplier = RandomUtils.nextIntInclude(50000, 100000);
        } else {
            // 5%: [100000, 1000000]
            crashMultiplier = RandomUtils.nextIntInclude(100000, 1000000);
        }
        log.debug("生成坠毁倍率: {} ({}x)", crashMultiplier, crashMultiplier / 10000.0);
        return crashMultiplier;
    }

    /**
     * 根据飞行经过时间计算当前倍率
     * <p>
     * 公式: multiplier = e^(rate * seconds)，其中 rate = growthRate / 10000.0
     * </p>
     *
     * @param elapsedMs  飞行经过的毫秒数
     * @param growthRate 增长速率（万分比），来自 AirRaidCfg.Growthmultiplier
     * @return 当前倍率（万分比）
     */
    public static int calculateCurrentMultiplier(long elapsedMs, int growthRate) {
        double rate = growthRate / 10000.0;
        double seconds = elapsedMs / 1000.0;
        double multiplier = Math.exp(rate * seconds);
        return (int) (multiplier * 10000);
    }

    /**
     * 根据坠毁倍率和增长速率计算飞行持续时间
     * <p>
     * 公式: seconds = ln(crashMultiplier / 10000.0) / rate
     * </p>
     *
     * @param crashMultiplier 坠毁倍率（万分比）
     * @param growthRate      增长速率（万分比），来自 AirRaidCfg.Growthmultiplier
     * @return 飞行持续时间（毫秒）
     */
    public static long calculateFlyDuration(int crashMultiplier, int growthRate) {
        double rate = growthRate / 10000.0;
        if (rate <= 0) {
            log.warn("增长速率 <= 0 ({}), 使用默认值 0.1", growthRate);
            rate = 0.1;
        }
        double targetMultiplier = crashMultiplier / 10000.0;
        double seconds = Math.log(targetMultiplier) / rate;
        return (long) (seconds * 1000);
    }
}
