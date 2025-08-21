package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lm
 * @date 2025/7/28 17:35
 */
public class BlackJackACEProcessorHandler implements IProcessorHandler {
    private static final Logger log = LoggerFactory.getLogger(BlackJackACEProcessorHandler.class);
    private final long id;

    private final BlackJackGameController gameController;

    public BlackJackACEProcessorHandler(long id, BlackJackGameController gameController) {
        this.id = id;
        this.gameController = gameController;
    }

    @Override
    public void action() throws Exception {
        BlackJackGameDataVo gameDataVo = gameController.getGameDataVo();
        if (gameDataVo.getId() != id) {
            log.info("购买ACE 定时器执行时id错过了");
            return;
        }
        if (gameController.getCurrentGamePhase() != EGamePhase.PLAY_CART) {
            return;
        }
        long aceBuyEndTime = gameDataVo.getAceBuyEndTime();
        if (aceBuyEndTime > 0) {
            gameController.notifyAceResult();
        }
    }
}
