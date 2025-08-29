package com.jjg.game.table.common.gamephase;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.manager.AbstractRoomManager;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.data.TableGameDataVo;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        if (gameController instanceof BaseTableGameController<D> controller)
            try {
                //踢未在线的玩家
                Map<Long, RoomPlayer> roomPlayers = controller.getRoom().getRoomPlayers();
                if (CollectionUtil.isNotEmpty(roomPlayers)) {
                    AbstractRoomManager roomManager = controller.getRoomController().getRoomManager();
                    Map<Long, PlayerController> playerControllers = controller.getRoomController().getPlayerControllers();
                    for (RoomPlayer roomPlayer : roomPlayers.values()) {
                        if (!roomPlayer.isOnline()) {
                            PlayerController playerController = playerControllers.get(roomPlayer.getPlayerId());
                            if (Objects.nonNull(playerController)) {
                                roomManager.exitRoom(playerController);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("结算踢不在线人异常", e);
            }
        try {
            phaseFinishAction();
        } catch (Exception e) {
            log.error("结算完成操作异常", e);
        }
    }

    public void phaseFinishAction() {

    }
}
