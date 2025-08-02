package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * @author lm
 * @date 2025/7/26 15:04
 */
public abstract class BasePlayCardPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {
    public BasePlayCardPhase(AbstractGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
    }


    @Override
    public void phaseFinish() {
        //正常逻辑不会执行到这儿 除非卡了 强制进行结算
    }

    @Override
    public int getPhaseRunTime() {
        return 150 * 1000;
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.PLAY_CART;
    }
}
