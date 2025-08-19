package com.jjg.game.table.common.utils;

import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.GameDataTracker;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase.SettlementData;

import java.util.List;
import java.util.Map;

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
        }
        // 添加流水数据
        gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.INCOME,
            settlementData.getBetWin());
        gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_BET,
            settlementData.getBetTotal());
        gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_WIN,
            settlementData.getTotalWin());
        // TODO 有效流水
        gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.EFFECTIVE_BET,
            settlementData.getBetTotal());
    }
}
