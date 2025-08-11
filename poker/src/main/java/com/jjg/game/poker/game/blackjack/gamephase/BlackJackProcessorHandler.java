package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;

import java.util.Objects;

/**
 * @author lm
 * @date 2025/7/28 17:35
 */
public class BlackJackProcessorHandler implements IProcessorHandler {

    private final long playerId;
    private final long id;
    private final BlackJackGameController gameController;

    public BlackJackProcessorHandler(long playerId, long id, BlackJackGameController gameController) {
        this.playerId = playerId;
        this.id = id;
        this.gameController = gameController;
    }

    @Override
    public void action() throws Exception {
        BlackJackGameDataVo gameDataVo = gameController.getGameDataVo();
        if (gameDataVo.getId() != id) {
            return;
        }
        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(currentPlayerSeatInfo) || currentPlayerSeatInfo.getPlayerId() != playerId) {
            return;
        }
        gameController.dealStopCard(playerId, PokerConstant.PlayerOperation.STOP);
    }
}
