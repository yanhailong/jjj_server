package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackCardInfo;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackSendCardInfo;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePlayCardPhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author lm
 * @date 2025/7/28 14:48
 */
public class BlackJackPlayCardPhase extends BasePlayCardPhase<BlackJackGameDataVo> {

    public BlackJackPlayCardPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void playCardPhaseDoAction() {
        if (gameController instanceof BlackJackGameController controller) {
            Room_ChessCfg roomChessCfg = gameDataVo.getRoomCfg();
            //发牌
            int poolId = BlackJackDataHelper.getPoolId(gameDataVo);
            Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(poolId);
            int sendNum = sendCards(cardListMap, gameDataVo);
            //发庄家
            List<Integer> dealerCards = gameDataVo.getCards().subList(0, roomChessCfg.getHandPoker());
            List<Integer> cards = new ArrayList<>(dealerCards);
            dealerCards.clear();
            gameDataVo.setDealerCards(cards);
            //通知发牌信息
            NotifyBlackJackSendCardInfo notifyBlackJackSendCardInfo = new NotifyBlackJackSendCardInfo();
            List<BlackJackCardInfo> blackJackCardInfos = new ArrayList<>();
            for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
                BlackJackCardInfo blackJackCardInfo = new BlackJackCardInfo();
                blackJackCardInfo.playerId = playerSeatInfo.getPlayerId();
                blackJackCardInfo.cardIds = BlackJackDataHelper.getClientId(playerSeatInfo.getCurrentCards(), poolId);
                //计算总点数
                blackJackCardInfo.totalPoint = BlackJackDataHelper.getTotalPoint(playerSeatInfo.getCurrentCards());
                blackJackCardInfos.add(blackJackCardInfo);
            }
            notifyBlackJackSendCardInfo.cardIdList = blackJackCardInfos;
            //添加庄家能显示的牌
            PokerCard pokerCard = cardListMap.get(gameDataVo.getDealerCards().get(0));
            notifyBlackJackSendCardInfo.cardId = pokerCard.getClientId();
            //如果庄家的牌包含A通知购买ACE
            boolean canBuy = pokerCard.getRank() == 1;
            if (canBuy) {
                // 添加定时器并设置购买ACE结束时间
                controller.addACETimer(sendNum + roomChessCfg.getHandPoker());
                gameDataVo.setAceBuyEndTime(gameDataVo.getPlayerTimerEvent().getNextTime());
            } else {
                //设置第一个开始的玩家 并添加定时
                PlayerSeatInfo first = gameDataVo.getPlayerSeatInfoList().get(gameDataVo.getIndex());
                controller.addNextTimer(first, sendNum + roomChessCfg.getHandPoker());
                notifyBlackJackSendCardInfo.operationId = first.getPlayerId();
            }
            notifyBlackJackSendCardInfo.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            gameDataVo.setCanBuyACE(canBuy);
            notifyBlackJackSendCardInfo.canBuyACE= canBuy;
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackSendCardInfo));
        }
    }

}
