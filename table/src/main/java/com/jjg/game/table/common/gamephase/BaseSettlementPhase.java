package com.jjg.game.table.common.gamephase;

import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.common.data.TableGameDataVo;

import java.util.List;

/**
 * 结算阶段
 *
 * @author 2CL
 */
public abstract class BaseSettlementPhase<D extends TableGameDataVo> extends AbstractRoomPhase<Room_BetCfg, D> {

    public BaseSettlementPhase(AbstractGameController<Room_BetCfg, D> gameController) {
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
     * 计算金币数量
     */
    protected long calcGold(WinPosWeightCfg weightCfg, long betValue) {
        int winRatio = gameDataVo.getRoomCfg().getWinRatio();
        // 倍率计算
        long multiAdd =
            (long) Math.floor(betValue * (weightCfg.getOdds() / 100.0) * ((
                10000 - (weightCfg.getIsRatio() == 1 ? winRatio : 0)) / 10000.0));
        long betReturn = (long) Math.floor(betValue * (weightCfg.getReturnRate() / 10000.0));
        // 赢的总值
        long totalWin = multiAdd + betReturn;
        log.info("玩家在压分区域：{}，押注：{}，获得： 赢 {} + 抽水返还 {}, 总值：{}"
            , weightCfg.getId(), betValue, multiAdd, betReturn, totalWin);
        // 倍率 + 压分返还
        return totalWin;
    }

    @Override
    public void phaseFinish() {
        // 检查机器人的退出概率

    }
}
