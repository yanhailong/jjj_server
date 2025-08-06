package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * @author lm
 * @date 2025/7/28 14:16
 */
public abstract class BaseBetPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {

    public BaseBetPhase(AbstractGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }


    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.BET;
    }

    @Override
    public int getPhaseRunTime() {
        return PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.BET);
    }

}
