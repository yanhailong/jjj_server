package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;


/**
 * 通用扑克等待游戏开始
 *
 * @author lm
 */
public class BaseWaitReadyPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {


    public BaseWaitReadyPhase(AbstractPhaseGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
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
