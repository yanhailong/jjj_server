package com.jjg.game.room.datatrack;


import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomDataHelper;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author lm
 * @date 2025/8/13 16:04
 */
public class SaveLogUtil {
    public static void generalLog(Map<Long, Map<Integer, List<Integer>>> betData, Map<Long, DefaultKeyValue<Long, Long>> playerGet, Map<Long, GamePlayer> gamePlayerMap, AbstractPhaseGameController<Room_BetCfg, ?> gameController) {
        Map<Integer, Long> areaTotalBet = new HashMap<>();
        GameDataTracker gameDataTracker = gameController.getGameDataTracker();
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : betData.entrySet()) {
            GamePlayer gamePlayer = gamePlayerMap.get(entry.getKey());
            if (Objects.isNull(gamePlayer) || gamePlayer instanceof GameRobotPlayer) {
                continue;
            }
            Map<Integer, List<Integer>> playerBetInfo = entry.getValue();
            DefaultKeyValue<Long, Long> keyValue = playerGet.get(entry.getKey());
            if (Objects.isNull(keyValue)) {
                keyValue = new DefaultKeyValue<>(0L, 0L);
            }
            long totalBet = 0;
            Map<Integer, Long> areaMap = new HashMap<>();
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
                areaTotalBet.merge(key, (long) sum, Long::sum);
                areaMap.put(key, (long) sum);
                totalBet += sum;
            }
            // 打点
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_BET, totalBet);
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_WIN, keyValue.getValue());
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.INCOME, keyValue.getValue() - totalBet);
            long sum = effectiveWaterFlow.values().stream().mapToLong(Math::abs).sum();
            if (sum > 0) {
                RoomDataHelper.checkPlayerVipLevel(gamePlayer, gameController, sum);
            }
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.EFFECTIVE_BET, sum);
            //添加活动进度
            long finalTotalBet = totalBet;
            Thread.ofVirtual().start(() -> {
                ActivityManager activityManager = gameController.getRoomController().getRoomManager().getActivityManager();
                if (sum > 0) {
                    activityManager.addPlayerActivityProgress(gamePlayer, ActivityTargetType.EFFECTIVE_BET.getTargetKey(), sum, gameController.getGameTransactionItemId());
                    activityManager.addActivityProgress(gamePlayer, ActivityTargetType.EFFECTIVE_BET.getTargetKey(), sum,gameController.getGameTransactionItemId());
                }
                activityManager.addPlayerActivityProgress(gamePlayer, ActivityTargetType.BET.getTargetKey(), finalTotalBet, gameController.getGameTransactionItemId());
            });
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.AREA_DATA, areaMap);
        }
        gameDataTracker.addGameLogData(DataTrackNameConstant.AREA_DATA, areaTotalBet);
    }

}
