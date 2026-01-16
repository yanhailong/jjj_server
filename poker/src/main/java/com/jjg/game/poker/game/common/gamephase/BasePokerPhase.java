package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.robot.RobotScheduleUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ChessRobotCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;


/**
 * @author lm
 * @date 2025/7/28 14:18
 */
public abstract class BasePokerPhase<T extends BasePokerGameDataVo> extends AbstractRoomPhase<Room_ChessCfg, T> {

    public BasePokerPhase(AbstractPhaseGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }

    public void nextPhase() {
    }


    @Override
    public void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer) {

    }

    /**
     * 机器人行为
     */
    public void robotAction() {
        //获取机器人 并进行下注
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            if (!seatInfo.isSeatDown()) {
                continue;
            }
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
            if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                ChessRobotCfg chessRobotCfg = GameDataManager.getChessRobotCfg(robotPlayer.getActionId());
                int chessExecutionDelay = RobotScheduleUtil.getChessExecutionDelay(robotPlayer.getActionId());
                int pro;
                if (robotPlayer.getLastWin() == 0) {
                    //刚刚加入房间
                    pro = 10000;
                } else {
                    pro = robotPlayer.getLastWin() == 1 ? chessRobotCfg.getContinueAfterVictory().getFirst() : chessRobotCfg.getContinueAfterFail().getFirst();
                }
                robotPhaseScheduleAction(robotPlayer, chessExecutionDelay, pro);
            }
        }
    }

    public void robotPhaseScheduleAction(GameRobotPlayer robotPlayer, int chessExecutionDelay, int pro) {

    }

    @Override
    public void onPlayerHalfwayExitPhase(GamePlayer gamePlayer) {

    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {

    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {

    }
}
