package com.jjg.game.room.datatrack.logdata;


import com.jjg.game.common.proto.Pair;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.GameDataTracker;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author lm
 * @date 2025/8/13 16:04
 */
public class SaveLogThread<T, F> implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SaveLogThread.class);
    private final LogParam<T, F> LogParam;
    private final Consumer<LogParam<T, F>> function;

    public SaveLogThread(LogParam<T, F> logParam, Consumer<LogParam<T, F>> function) {
        LogParam = logParam;
        this.function = function;
    }

    @Override
    public void run() {
        try {
            function.accept(LogParam);
        } catch (Exception e) {
            log.error("发送游戏日志失败", e);
        }
    }

    public static <K> void generalLog(LogParam<Pair<Map<Long, Map<Integer, List<Integer>>>, Map<Long, DefaultKeyValue<Long, Long>>>, K> param, GameDataTracker gameDataTracker) {
        Pair<Map<Long, Map<Integer, List<Integer>>>, Map<Long, DefaultKeyValue<Long, Long>>> dataParam = param.param();
        Map<Long, Map<Integer, List<Integer>>> betData = dataParam.getFirst();
        Map<Long, DefaultKeyValue<Long, Long>> playerGet = dataParam.getSecond();
        Map<Integer, Long> areaTotalBet = new HashMap<>();
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : betData.entrySet()) {
            GamePlayer gamePlayer = param.playerData().get(entry.getKey());
            if (Objects.isNull(gamePlayer)) {
                continue;
            }
            Map<Integer, List<Integer>> playerBetInfo = entry.getValue();
            DefaultKeyValue<Long, Long> keyValue = playerGet.get(entry.getKey());
            long totalBet = 0;
            for (Map.Entry<Integer, List<Integer>> listEntry : playerBetInfo.entrySet()) {
                List<Integer> value = listEntry.getValue();
                int sum = value.stream().mapToInt(Integer::intValue).sum();
                areaTotalBet.merge(listEntry.getKey(), (long) sum, Long::sum);
                gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.AREA_ID, listEntry.getKey());
                gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.AREA_TOTAL_BET, sum);
                totalBet += sum;
            }
            // 打点
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_BET, totalBet);
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_WIN, keyValue.getValue());
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.INCOME, keyValue.getValue() - totalBet);

        }
        for (Map.Entry<Integer, Long> entry : areaTotalBet.entrySet()) {
            gameDataTracker.addGameLogData(DataTrackNameConstant.AREA_ID, entry.getKey());
            gameDataTracker.addGameLogData(DataTrackNameConstant.AREA_TOTAL_BET, entry.getValue());
        }
    }
}
