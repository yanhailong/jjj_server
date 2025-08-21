package com.jjg.game.room.datatrack;


import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
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
    public static void generalLog(Map<Long, Map<Integer, List<Integer>>> betData, Map<Long, DefaultKeyValue<Long, Long>> playerGet, Map<Long, GamePlayer> gamePlayerMap, GameDataTracker gameDataTracker) {
        Map<Integer, Long> areaTotalBet = new HashMap<>();
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : betData.entrySet()) {
            GamePlayer gamePlayer = gamePlayerMap.get(entry.getKey());
            if (Objects.isNull(gamePlayer)) {
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
                effectiveWaterFlow.put(key, sum - bet);
                areaTotalBet.merge(key, (long) sum, Long::sum);
                areaMap.put(key, (long) sum);
                totalBet += sum;
            }
            // 打点
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_BET, totalBet);
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_WIN, keyValue.getValue());
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.INCOME, keyValue.getValue() - totalBet);
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.EFFECTIVE_BET, effectiveWaterFlow.values().stream().mapToLong(Math::abs).sum());
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.AREA_DATA, areaMap);
        }
        gameDataTracker.addGameLogData(DataTrackNameConstant.AREA_DATA, areaTotalBet);
    }
}
