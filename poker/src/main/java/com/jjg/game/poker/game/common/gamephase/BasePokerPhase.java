package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * @author lm
 * @date 2025/7/28 14:18
 */
public abstract class BasePokerPhase<T extends BasePokerGameDataVo> extends AbstractRoomPhase<Room_ChessCfg, T> {

    public BasePokerPhase(AbstractGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }

    public void nextPhase() {
    }


    @Override
    public void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer) {

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
