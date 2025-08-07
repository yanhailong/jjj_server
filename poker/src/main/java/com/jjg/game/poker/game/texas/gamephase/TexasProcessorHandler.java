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

    private final long playerId;
    private final long id;
    private final TexasGameController gameController;
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
            //容错处理


            return;
        }
        //①翻牌前圈，弃/过，优先执行弃牌；②翻牌圈开始及后续每轮次，弃/过，优先执行过牌；
        ReqPokerBet reqPokerBet = new ReqPokerBet();
        reqPokerBet.betType = 6;
        gameController.dealBet(playerId, reqPokerBet);
        //TODO
//        if (gameDataVo.getRound() == 1) {
//            //优先弃牌
//            gameController.discardCard(playerId);
//        } else {
//            if (!gameController.passCards(playerId)) {
//                gameController.discardCard(playerId);
//            }
//        }
    }
}
