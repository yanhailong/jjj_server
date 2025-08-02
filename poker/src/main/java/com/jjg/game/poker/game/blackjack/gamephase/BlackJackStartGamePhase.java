//package com.jjg.game.poker.game.blackjack.gamephase;
//
//import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
//import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
//import com.jjg.game.poker.game.common.BasePokerGameController;
//import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
//
///**
// * @author lm
// * @date 2025/7/28 14:05
// */
//public class BlackJackStartGamePhase extends BaseStartGamePhase<BlackJackGameDataVo> {
//
//
//    public BlackJackStartGamePhase(BlackJackGameController gameController) {
//        super(gameController);
//    }
//
//    @Override
//    public void nextPhase() {
//        //玩家进行下注阶段
//        if (gameController instanceof BasePokerGameController<BlackJackGameDataVo> gameC) {
//            BlackJackBetPhase currentGamePhase = new BlackJackBetPhase(gameController);
//            gameC.addPokerPhaseTimer(currentGamePhase);
//        }
//    }
//
//}
