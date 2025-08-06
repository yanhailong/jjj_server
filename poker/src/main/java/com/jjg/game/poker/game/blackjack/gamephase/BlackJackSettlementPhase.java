package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;

/**
 * @author lm
 * @date 2025/7/29 09:31
 */
public class BlackJackSettlementPhase extends BaseSettlementPhase<BlackJackGameDataVo> {

    public BlackJackSettlementPhase(BlackJackGameController gameController) {
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
