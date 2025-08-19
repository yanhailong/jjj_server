package com.jjg.game.table.common.gamephase;

import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.data.TableGameDataVo;

import java.util.List;

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
        // 倍率计算
        long multiAdd =
            (long) Math.floor(betValue * (weightCfg.getOdds() / 100.0) * ((
                10000 - (weightCfg.getIsRatio() == 1 ? winRatio : 0)) / 10000.0));
        long betReturn = (long) Math.floor(betValue * (weightCfg.getReturnRate() / 10000.0));
        // 赢的总值
        long totalWin = multiAdd + betReturn;
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
        return new SettlementData(multiAdd, betReturn, totalWin, betValue);
    }

    @Override
    public void phaseFinish() {
        // 检查机器人的退出概率
    }

    /**
     * 结算数据
     */
    public static class SettlementData {
        // 玩家压分的净赢值
        private long betWin;
        // 返还压分
        private long betReturn;
        // 赢的总值
        private long totalWin;
        // 压的总值
        private long betTotal;

        public SettlementData() {
        }

        public SettlementData(long betWin, long betReturn, long totalWin, long betTotal) {
            this.betWin = betWin;
            this.betReturn = betReturn;
            this.totalWin = totalWin;
            this.betTotal = betTotal;
        }

        public long getBetWin() {
            return betWin;
        }

        public long getBetReturn() {
            return betReturn;
        }

        public long getTotalWin() {
            return totalWin;
        }

        public long getBetTotal() {
            return betTotal;
        }

        public void setBetTotal(long betTotal) {
            this.betTotal = betTotal;
        }

        /**
         * 增加结算值
         */
        public void increaseBySettlementData(SettlementData settlementData) {
            this.betWin += settlementData.betWin;
            this.betReturn += settlementData.betReturn;
            this.totalWin += settlementData.totalWin;
            this.betTotal += settlementData.betTotal;
        }
    }
}
