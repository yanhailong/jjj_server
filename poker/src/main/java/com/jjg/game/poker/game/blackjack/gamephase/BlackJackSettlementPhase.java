package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * @author lm
 * @date 2025/7/29 09:31
 */
public class BlackJackSettlementPhase extends BaseSettlementPhase<BlackJackGameDataVo> {

    public BlackJackSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();

    }

    @Override
    public void nextPhase() {

    }

    @Override
    public void phaseFinish() {

    }


    @Override
    public int getMaxExecuteTime() {
        return super.getMaxExecuteTime();
    }
}
