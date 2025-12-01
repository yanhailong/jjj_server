package com.jjg.game.table.common.gamephase;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.data.TableGameDataVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 结算阶段
 *
 * @author 2CL
 */
public abstract class BaseSettlementPhase<D extends TableGameDataVo> extends AbstractRoomPhase<Room_BetCfg, D> {

    public BaseSettlementPhase(AbstractPhaseGameController<Room_BetCfg, D> gameController) {
        super(gameController);
    }

    @Override
    public int getPhaseRunTime() {
        List<Integer> stageTime = gameDataVo.getRoomCfg().getStageTime();
        if (stageTime.size() >= 4) {
            return stageTime.get(2) + stageTime.get(3);
        }
        return 0;
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {

    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {

    }

    @Override
    public void onPlayerHalfwayExitPhase(GamePlayer gamePlayer) {

    }

    @Override
    public void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer) {

    }

    /**
     * 计算金币数量, 需要减去押注的钱
     */
    protected SettlementData calcGold(GamePlayer gamePlayer, WinPosWeightCfg weightCfg, long betValue) {
        int winRatio = gameDataVo.getRoomCfg().getWinRatio();
//        // 倍率计算
//        long totalGet = betValue * (weightCfg.getOdds() / 100);
//        long multiAdd = (long) Math.floor(totalGet * ((10000 - (weightCfg.getIsRatio() == 1 ? winRatio : 0)) / 10000.0));
//        long betReturn = (long) Math.floor(betValue * (weightCfg.getReturnRate() / 10000.0));
//        // 赢的总值
//        long totalWin = multiAdd + betReturn;
        // 倍率计算
        BigDecimal totalGet = BigDecimal.valueOf(betValue)
                .multiply(BigDecimal.valueOf(weightCfg.getOdds()))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);
        BigDecimal multiAdd = totalGet.multiply(BigDecimal.valueOf((10000 - (weightCfg.getIsRatio() == 1 ? winRatio : 0))))
                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.DOWN);
        long betReturn = BigDecimal.valueOf(betValue).multiply(BigDecimal.valueOf(weightCfg.getReturnRate()))
                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();
        // 赢的总值
        long totalWin = multiAdd.longValue() + betReturn;
        if (!(gamePlayer instanceof GameRobotPlayer)) {
            log.info("玩家：{} {} 在压分区域：{}，押注：{}，获得： 赢 {} + 抽水返还 {}, 总值：{}",
                    gamePlayer.getId(),
                    gameDataVo.roomLogInfo(),
                    weightCfg.getId(),
                    betValue,
                    multiAdd,
                    betReturn,
                    totalWin);
        }
        // 倍率计算 + 压分返还 + 赢的总值
        return new SettlementData(multiAdd.longValue(), betReturn, totalWin, betValue, totalGet.longValue() - multiAdd.longValue());
    }

    /**
     * 获取RoomBankerChangeParam
     * @param betInfo 下注信息
     * @return RoomBankerChangeParam
     */
    public RoomBankerChangeParam getRoomBankerChangeParam(Map<Integer, Map<Long, List<Integer>>> betInfo) {
        RoomBankerChangeParam param = new RoomBankerChangeParam();
        if (gameController.getRoom() instanceof FriendRoom && CollectionUtil.isNotEmpty(betInfo)) {
            param.initData(gameDataVo.getBetInfo());
            return param;
        }
        return param;
    }

    /**
     * 计算房主应得的收益
     */
    protected long calcRoomCreatorIncome(long totalTaxRevenue) {
        RoomType roomType = RoomType.getRoomType(gameDataVo.getRoomCfg().getId());
        long bankerIncome = 0;
        // 如果是好友房需要扣除一部分金币给房主
        if (roomType == RoomType.POKER_TEAM_UP_ROOM || roomType == RoomType.BET_TEAM_UP_ROOM) {
            // 庄家扣税比例
            int bankerIncomeRatio = SampleDataUtils.getIntGlobalData(GlobalSampleConstantId.CREATE_ROOM_FUNC_INCOME_RATIO);
            bankerIncome = totalTaxRevenue * bankerIncomeRatio / 10000;
            log.info("房主：{} 收益：{}", gameController.getRoom().getCreator(), bankerIncome);
        }
        return bankerIncome;
    }

    /**
     * 计算最后的BankerChange
     */
    public void calculationFinalBankerChange(RoomBankerChangeParam param) {
        gameDataTracker.addGameLogData("tax", param.getTotalTaxRevenue());
        if (gameController.getRoom() instanceof FriendRoom) {
            long totalGet = 0;
            for (Map.Entry<Integer, Map<Long, Integer>> entry : param.getBankerChangeMap().entrySet()) {
                long sum = entry.getValue().values().stream().mapToLong(Integer::intValue).sum();
                long realGet = sum * gameDataVo.getRoomCfg().getEffectiveRatio() / 10000;
                param.addTotalTaxRevenue(sum - realGet);
                totalGet += realGet;
            }
            param.addBankerChangeGold(-totalGet);
            //计算房主收益
            param.addRoomCreatorTotalIncome(calcRoomCreatorIncome(param.getTotalTaxRevenue()));
        }
    }


    @Override
    public void phaseFinish() {
        //结算完成直接清除数据
        gameDataVo.clearRoundData(gameController);
        // 检查机器人的退出概率
        try {
            phaseFinishAction();
        } catch (Exception e) {
            log.error("结算完成操作异常", e);
        }
    }

    public void phaseFinishAction() {

    }
}
