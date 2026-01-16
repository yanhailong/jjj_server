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
        if (gamePlayer == null || gamePlayer instanceof GameRobotPlayer) {
            return;
        }
        GameDataTracker gameDataTracker = controller.getGameDataTracker();
        long income = 0;
        if (playerBetInfo != null) {
            // 统计总押注
            settlementData.setBetTotal(playerBetInfo.values().stream()
                    .mapToLong(a -> a.stream().mapToInt(b -> b).sum())
                    .sum());
            income = settlementData.getTotalWin() - settlementData.getBetTotal();
            long effectiveWaterFlow = controller.calculationEffectiveWaterFlow(playerBetInfo);
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.EFFECTIVE_BET, effectiveWaterFlow);
            //添加活动进度
            controller.getRoomController().getRoomProcessor().tryPublish(0, new BaseHandler<String>() {
                @Override
                public void action() {
                    SaveLogUtil.dealEffectiveWaterFlow(controller, gamePlayer, effectiveWaterFlow, settlementData.getBetTotal());
                    if (settlementData.getTotalWin() <= 0) {
                        controller.dealLose(gamePlayer, settlementData.getBetTotal());
                    }
                }
            }.setHandlerParamWithSelf("recordBetLog"));
            controller.triggerSettlementAction(gamePlayer.getId(), controller.getRoom().getGameType(), effectiveWaterFlow, income, controller.getGameTransactionItemId());
        }
        // 添加流水数据
        gameDataTracker.addPlayerLogData(
                gamePlayer, DataTrackNameConstant.INCOME, income);
        gameDataTracker.addPlayerLogData(
                gamePlayer, DataTrackNameConstant.TOTAL_BET, settlementData.getBetTotal());
        gameDataTracker.addPlayerLogData(
                gamePlayer, DataTrackNameConstant.TOTAL_WIN, settlementData.getTotalWin());
    }


}
