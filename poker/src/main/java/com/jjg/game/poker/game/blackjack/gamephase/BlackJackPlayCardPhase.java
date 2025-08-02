//package com.jjg.game.poker.game.blackjack.gamephase;
//
//import com.jjg.game.core.utils.PokerCardUtils;
//import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
//import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
//import com.jjg.game.poker.game.common.BasePokerGameController;
//import com.jjg.game.poker.game.common.PokerBuilder;
//import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
//import com.jjg.game.poker.game.common.gamephase.BasePlayCardPhase;
//import com.jjg.game.poker.game.common.message.reps.NotifyPlayerOperate;
//import com.jjg.game.room.constant.EGamePhase;
//import com.jjg.game.room.message.RoomMessageBuilder;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import static com.jjg.game.poker.game.blackjack.constant.BlackJackConstant.Common.INIT_CARD_NUM;
//
///**
// * @author lm
// * @date 2025/7/28 14:48
// */
//public class BlackJackPlayCardPhase extends BasePlayCardPhase<BlackJackGameDataVo> {
//    public BlackJackPlayCardPhase(BlackJackGameController gameController) {
//        super(gameController);
//    }
//
//    @Override
//    public void phaseDoAction() {
//        super.phaseDoAction();
//        //发牌
//        gameDataVo.setCards(PokerCardUtils.getPokerIntIdExceptJoker());
//        List<PlayerSeatInfo> playerSeatInfo = gameDataVo.getPlayerSeatInfoList();
//        List<Integer> cards = gameDataVo.getCards();
//        Collections.shuffle(cards);
//        //从第一个执行者开始发牌
//        for (PlayerSeatInfo info : playerSeatInfo) {
//            List<Integer> playCard = cards.subList(0, INIT_CARD_NUM);
//            List<List<Integer>> list = new ArrayList<>();
//            list.add(playCard);
//            info.setCards(list);
//            playCard.clear();
//        }
//        //发庄家
//        PlayerSeatInfo master = gameDataVo.getMaster();
//        List<Integer> playCard = cards.subList(0, INIT_CARD_NUM);
//        List<List<Integer>> list = new ArrayList<>();
//        list.add(playCard);
//        master.setCards(list);
//        playCard.clear();
//        //设置第一个开始的玩家 并添加定时
//        PlayerSeatInfo first = gameDataVo.getPlayerSeatInfoList().get(gameDataVo.getIndex());
//        gameDataVo.setRound(gameDataVo.getRound() + 1);
//        if (gameController instanceof BlackJackGameController blackJackGameController) {
//            NotifyPlayerOperate notifyPlayerOperate = blackJackGameController.notifyNextPlayer(blackJackGameController, first, new NotifyPlayerOperate());
//            blackJackGameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPlayerOperate));
//        }
//    }
//
//
//    @Override
//    public void phaseFinish() {
//        //进入结算
//    }
//
//    @Override
//    public void nextPhase() {
//        //设置当前游戏阶段为发牌
//        if (gameController instanceof BlackJackGameController controller) {
//            BlackJackSettlementPhase gamePhase = new BlackJackSettlementPhase(controller);
//            controller.addPokerPhaseTimer(gamePhase);
//            //通知场上信息
//            PokerBuilder.buildNotifyPhaseChange(EGamePhase.GAME_ROUND_OVER_SETTLEMENT, gameDataVo.getPhaseEndTime());
//        }
//    }
//}
