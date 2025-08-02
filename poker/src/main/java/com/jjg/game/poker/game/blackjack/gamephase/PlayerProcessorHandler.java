//package com.jjg.game.poker.game.blackjack.gamephase;
//
//import com.jjg.game.common.concurrent.IProcessorHandler;
//import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
//import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
//import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
//import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
//import com.jjg.game.poker.game.common.message.reps.NotifyPlayerOperate;
//import com.jjg.game.poker.game.common.message.req.ReqPlayerOperate;
//import com.jjg.game.room.message.RoomMessageBuilder;
//
///**
// * @author lm
// * @date 2025/7/28 17:35
// */
//public class PlayerProcessorHandler implements IProcessorHandler {
//
//    private final long playerId;
//    private final int round;
//    private final BlackJackGameController gameController;
//
//    public PlayerProcessorHandler(long playerId, int round, BlackJackGameController gameController) {
//        this.playerId = playerId;
//        this.round = round;
//        this.gameController = gameController;
//    }
//
//    @Override
//    public void action() throws Exception {
//        BlackJackGameDataVo gameDataVo = gameController.getGameDataVo();
//        if (gameDataVo.getRound() != round) {
//            return;
//        }
//        PlayerSeatInfo currentPlayerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
//        if (currentPlayerSeatInfo.getPlayerId() != playerId) {
//            return;
//        }
//        ReqPlayerOperate reqPlayerOperate = new ReqPlayerOperate();
//        reqPlayerOperate.operateType = BlackJackConstant.Operation.STOP_CARD;
//        gameController.dealStopCard(playerId,reqPlayerOperate);
//    }
//}
