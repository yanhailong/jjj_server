package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

/**
 * @author lm
 * @date 2025/7/28 14:54
 */
public class BlackJackWaitReadyPhase extends BaseWaitReadyPhase<BlackJackGameDataVo> {
    public BlackJackWaitReadyPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController) {
        super(gameController);
    }
}
