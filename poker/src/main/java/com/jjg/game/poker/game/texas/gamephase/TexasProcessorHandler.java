package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;

import java.util.Objects;

/**
 * @author lm
 * @date 2025/7/28 17:35
 */
public class TexasProcessorHandler implements IProcessorHandler {
    //玩家id
    private final long playerId;
    //对局id
    private final long id;
    private final TexasGameController gameController;
    //本局玩家定时器id
    private final int timerId;

    public TexasProcessorHandler(long playerId, long id, TexasGameController gameController, int timerId) {
        this.playerId = playerId;
        this.id = id;
        this.gameController = gameController;
        this.timerId = timerId;
    }

    @Override
    public void action() throws Exception {
        TexasGameDataVo gameDataVo = gameController.getGameDataVo();
        if (id != gameDataVo.getId() || timerId != gameDataVo.getTimerId()) {
            return;
        }
        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (Objects.isNull(currentPlayerSeatInfo) || currentPlayerSeatInfo.getPlayerId() != playerId) {
            return;
        }
        //①翻牌前圈，弃/过，优先执行弃牌；②翻牌圈开始及后续每轮次，弃/过，优先执行过牌；
        if (gameDataVo.getRound() == 1) {
            //优先弃牌
            gameController.discardCard(playerId);
        } else {
            if (!gameController.passCards(playerId)) {
                gameController.discardCard(playerId);
            }
        }
    }
}
