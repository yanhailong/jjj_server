package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.Map;

/**
 * 通用扑克等待游戏开始
 *
 * @author lm
 */
public class BaseWaitReadyPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {


    public BaseWaitReadyPhase(AbstractGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }

    @Override
    public void nextPhase() {

    }

    @Override
    public void phaseDoAction() {
    }

    @Override
    public void phaseFinish() {
    }

    @Override
    public int getPhaseRunTime() {
        return -1;
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.WAIT_READY;
    }
}
