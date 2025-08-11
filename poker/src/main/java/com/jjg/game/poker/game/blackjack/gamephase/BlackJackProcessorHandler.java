package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BasePokerProcessorHandler;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.room.message.RoomMessageBuilder;

/**
 * @author lm
 * @date 2025/7/28 17:35
 */
public class BlackJackProcessorHandler extends BasePokerProcessorHandler<BlackJackGameDataVo> {

    public BlackJackProcessorHandler(long playerId, long id, BlackJackGameController gameController) {
        super(playerId, id, gameController);
    }

    @Override
    public void addNextPlayer(PlayerSeatInfo nextPlayerSeatInfo) {
        if (getGameController() instanceof BlackJackGameController controller) {
            controller.addNextTimer(nextPlayerSeatInfo, 0);
            NotifyPokerSampleCardOperation notifyPokerSampleCardOperation = new NotifyPokerSampleCardOperation();
            notifyPokerSampleCardOperation.nextPlayerId = nextPlayerSeatInfo.getPlayerId();
            notifyPokerSampleCardOperation.overTime = controller.getGameDataVo().getPlayerTimerEvent().getNextTime();
            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPokerSampleCardOperation));
        }
    }

    @Override
    public void dealAction() {
        if (getGameController() instanceof BlackJackGameController controller) {
            controller.dealStopCard(getPlayerId(), PokerConstant.PlayerOperation.STOP);
        }
    }
}
