package com.jjg.game.ploy.games.airraid.manager;

import com.jjg.game.ploy.games.airraid.AirRaidCrashCalculator;
import com.jjg.game.ploy.games.airraid.data.AirRaidGameRoom;
import com.jjg.game.ploy.games.airraid.data.AirRaidPhase;
import com.jjg.game.ploy.games.airraid.data.AirRaidRuleConfig;
import org.springframework.stereotype.Component;

/**
 * AirRaid 阶段状态相关的纯函数集合
 *
 * @author 11
 * @date 2026/5/14
 */
@Component
public class AirRaidPhaseStateManager {
    /**
     * 计算阶段停止时间
     *
     * @param phaseDurationMs
     * @return
     */
    public long resolvePhaseStopTime(AirRaidGameRoom gameRoom, long phaseDurationMs) {
        long stopTime;
        if (gameRoom.getPhase() == AirRaidPhase.FLYING) {
            stopTime = gameRoom.getPhaseStopTime();
        } else {
            stopTime = phaseDurationMs > 0 ? gameRoom.getPhaseStartTime() + phaseDurationMs : 0;
        }
        gameRoom.setPhaseStopTime(stopTime);
        return stopTime;
    }

    /**
     * 计算当前的倍数
     *
     * @param now
     * @return
     */
    public int getAuthoritativeCurrentMultiplier(AirRaidGameRoom gameRoom, AirRaidRuleConfig airRaidRuleConfig, long now) {
        if (gameRoom.getPhase() != AirRaidPhase.FLYING) {
            return gameRoom.getCurrentMultiplier();
        }
        long stopTime = gameRoom.getPhaseStopTime();
        if (stopTime > 0 && now >= stopTime) {
            return gameRoom.getCrashMultiplier();
        }
        long flyStartTime = gameRoom.getPhaseStartTime();
        if (flyStartTime <= 0) {
            return gameRoom.getCurrentMultiplier();
        }
        int multiplier = AirRaidCrashCalculator.calculateCurrentMultiplier(now - flyStartTime, airRaidRuleConfig.getGrowthRate());
        gameRoom.setCurrentMultiplier(multiplier);
        return multiplier;
    }
}
