package com.jjg.game.table.common.utils;

import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.data.room.RoomDataHelper;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.GameDataTracker;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 押注类日志打点工具
 *
 * @author 2CL
 */
public class BetDataTrackLogUtils {


    /**
     * 记录押注日志
     */
    public static void recordBetLog(
            SettlementData settlementData, GamePlayer gamePlayer, GameDataTracker gameDataTracker,
            Map<Integer, List<Integer>> playerBetInfo) {
        if (settlementData.getBetTotal() <= 0 && playerBetInfo != null) {
            // 统计总押注
            settlementData.setBetTotal(
                    playerBetInfo.values().stream().mapToLong(a -> a.stream().mapToInt(b -> b).sum()).sum());
            long effectiveWaterFlow = calculationEffectiveWaterFlow(playerBetInfo);
            if (effectiveWaterFlow > 0) {
                RoomDataHelper.checkPlayerVipLevel(gamePlayer, effectiveWaterFlow);
            }
            gameDataTracker.addPlayerLogData(
                    gamePlayer, DataTrackNameConstant.EFFECTIVE_BET, effectiveWaterFlow);
        }
        // 添加流水数据
        gameDataTracker.addPlayerLogData(
                gamePlayer, DataTrackNameConstant.INCOME, settlementData.getBetWin());
        gameDataTracker.addPlayerLogData(
                gamePlayer, DataTrackNameConstant.TOTAL_BET, settlementData.getBetTotal());
        gameDataTracker.addPlayerLogData(
                gamePlayer, DataTrackNameConstant.TOTAL_WIN, settlementData.getTotalWin());

    }

    public static long calculationEffectiveWaterFlow(Map<Integer, List<Integer>> playerBetInfo) {
        Map<Integer, Long> effectiveWaterFlow = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> listEntry : playerBetInfo.entrySet()) {
            Integer key = listEntry.getKey();
            BetAreaCfg betAreaCfg = GameDataManager.getBetAreaCfg(key);
            if (Objects.isNull(betAreaCfg)) {
                continue;
            }
            List<Integer> value = listEntry.getValue();
            int sum = value.stream().mapToInt(Integer::intValue).sum();
            //计算有效流水
            Long bet = effectiveWaterFlow.getOrDefault(betAreaCfg.getRepulsionID(), 0L);
            if (bet > 0) {
                effectiveWaterFlow.put(betAreaCfg.getRepulsionID(), sum - bet);
            } else {
                effectiveWaterFlow.put(key, (long) sum);
            }
        }
        return effectiveWaterFlow.values().stream().mapToLong(Math::abs).sum();
    }


}
