package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BasePokerProcessorHandler;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;

/**
 * @author lm
 * @date 2025/7/28 17:35
 */
public class TexasProcessorHandler extends BasePokerProcessorHandler<TexasGameDataVo> {

    //本局玩家定时器id
    private final int timerId;

    public TexasProcessorHandler(long playerId, long id, TexasGameController gameController, int timerId) {
        super(playerId, id, gameController);
        this.timerId = timerId;
    }

    @Override
    public void addNextPlayer(PlayerSeatInfo nextPlayerSeatInfo) {
        if (getGameController() instanceof TexasGameController controller) {
            controller.addNextPlayerAndBroadcast(nextPlayerSeatInfo, new NotifyPokerSampleCardOperation());
        }
    }

    @Override
    public void dealAction() {
        if (getGameController() instanceof TexasGameController controller) {
            TexasGameDataVo gameDataVo = controller.getGameDataVo();
            long playerId = getPlayerId();
            if (timerId != gameDataVo.getTimerId()) {
                return;
            }
            //①翻牌前圈，弃/过，优先执行弃牌；②翻牌圈开始及后续每轮次，弃/过，优先执行过牌；
            if (gameDataVo.getRound() == 1) {
                //优先弃牌
                controller.discardCard(playerId);
            } else {
                if (!controller.passCards(playerId)) {
                    controller.discardCard(playerId);
                }
            }
        }
    }
}
