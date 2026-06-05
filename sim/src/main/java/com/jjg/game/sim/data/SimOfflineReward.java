package com.jjg.game.sim.data;

import com.jjg.game.sim.constant.BuildingOutputType;

import java.util.Map;

/**
 * 待领取的离线收益快照 (运行时, 上线计算, 领取后清空)
 *
 * @author 11
 * @date 2026/5/29
 */
public class SimOfflineReward {
    //1倍基础奖励 itemId -> 数量
    private final Map<BuildingOutputType, Long> baseReward;
    //有效结算时长(分)
    private final int effectiveMinutes;
    //当前经营等级对应的离线上限时长(分)
    private final int capMinutes;
    //广告倍数 (上线时确定, 领取时使用)
    private final String adMultiplier;

    public SimOfflineReward(Map<BuildingOutputType, Long> baseReward, int effectiveMinutes, int capMinutes, String adMultiplier) {
        this.baseReward = baseReward;
        this.effectiveMinutes = effectiveMinutes;
        this.capMinutes = capMinutes;
        this.adMultiplier = adMultiplier;
    }

    public Map<BuildingOutputType, Long> getBaseReward() {
        return baseReward;
    }

    public int getEffectiveMinutes() {
        return effectiveMinutes;
    }

    public int getCapMinutes() {
        return capMinutes;
    }

    public String getAdMultiplier() {
        return adMultiplier;
    }
}
