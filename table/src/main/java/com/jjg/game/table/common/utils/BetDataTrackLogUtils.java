package com.jjg.game.table.common.utils;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.GameDataTracker;
import com.jjg.game.room.datatrack.SaveLogUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;

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


    public static void main(String[] args) {
        Map<Integer, List<Integer>> playerBetInfo = new HashMap<>();
        playerBetInfo.put(1, List.of(100, 200, 300));
        playerBetInfo.put(2, List.of(100, 200, 300));
        playerBetInfo.put(3, List.of(100, 200, 300));
        System.out.println(playerBetInfo.values().stream()
                .mapToLong(a -> a.stream().mapToInt(b -> b).sum())
                .sum());
    }


    /**
     * 记录押注日志
     */
    public static void recordBetLog(SettlementData settlementData, GamePlayer gamePlayer,
                                    AbstractPhaseGameController<Room_BetCfg, ?> controller, Map<Integer, List<Integer>> playerBetInfo) {
        GameDataTracker gameDataTracker = controller.getGameDataTracker();
        if (playerBetInfo != null) {
            // 统计总押注
            settlementData.setBetTotal(playerBetInfo.values().stream()
                    .mapToLong(a -> a.stream().mapToInt(b -> b).sum())
                    .sum());
            if (!(gamePlayer instanceof GameRobotPlayer)) {
                long effectiveWaterFlow = calculationEffectiveWaterFlow(playerBetInfo);
                gameDataTracker.addPlayerLogData(
                        gamePlayer, DataTrackNameConstant.EFFECTIVE_BET, effectiveWaterFlow);
                //添加活动进度
                controller.getRoomController().getRoomProcessor().tryPublish(0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        SaveLogUtil.dealEffectiveWaterFlow(controller, gamePlayer, settlementData.getBetTotal(), settlementData.getBetWin());
                        if (settlementData.getTotalWin() <= 0) {
                            controller.dealLose(gamePlayer, settlementData.getBetTotal());
                        }
                    }
                }.setHandlerParamWithSelf("recordBetLog"));
            }
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
