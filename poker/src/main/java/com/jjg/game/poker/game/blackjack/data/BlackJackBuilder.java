package com.jjg.game.poker.game.blackjack.data;

import com.jjg.game.poker.game.blackjack.message.bean.BlackJackCardInfo;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackPlayerInfo;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackBetResult;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackPutCard;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.texas.data.SeatInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author lm
 * @date 2025/8/6 14:55
 */
public class BlackJackBuilder {
    private BlackJackBuilder() {
    }

    public static NotifyBlackJackPutCard getNotifyBlackJackPutCard(long nextPlayerId, PlayerSeatInfo oldInfo, BlackJackGameDataVo gameDataVo, int cardId) {
        return getNotifyBlackJackPutCard(nextPlayerId, oldInfo, gameDataVo, cardId, 0);
    }

    public static NotifyBlackJackPutCard getNotifyBlackJackPutCard(long nextPlayerId, PlayerSeatInfo oldInfo, BlackJackGameDataVo gameDataVo, int cardId, int autoCardId) {
        NotifyBlackJackPutCard jackPutCard = new NotifyBlackJackPutCard();
        if (nextPlayerId > 0) {
            jackPutCard.operationId = nextPlayerId;
            jackPutCard.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        }
        jackPutCard.cardId = BlackJackDataHelper.getClientCardId(gameDataVo, cardId);
        jackPutCard.cardIndex = oldInfo.getCards().size() > 1 && autoCardId == 0 ? oldInfo.getCardIndex() + 1 : oldInfo.getCardIndex();
        if (autoCardId > 0) {
            jackPutCard.autoCardId = BlackJackDataHelper.getClientCardId(gameDataVo, autoCardId);
            jackPutCard.nextTotalPoint = BlackJackDataHelper.getShowTotalPoint(oldInfo.getCards().getLast());
        }
        jackPutCard.playerId = oldInfo.getPlayerId();
        jackPutCard.totalPoint = BlackJackDataHelper.getShowTotalPoint(oldInfo.getCards().getFirst());
        return jackPutCard;
    }


    public static BlackJackPlayerInfo getBlackJackPlayerInfo(PlayerSeatInfo playerSeatInfo, SeatInfo seatInfo, BlackJackGameController controller) {
        BlackJackPlayerInfo blackJackPlayerInfo = new BlackJackPlayerInfo();
        BlackJackGameDataVo gameDataVo = controller.getGameDataVo();
        PokerPlayerInfo pokerPlayerInfo = PokerBuilder.getPokerPlayerInfo(seatInfo, gameDataVo);
        if (Objects.nonNull(playerSeatInfo)) {
            blackJackPlayerInfo.currentCardIds = playerSeatInfo.getCardIndex();
            pokerPlayerInfo.operationType = playerSeatInfo.getOperationType();
            if (Objects.nonNull(playerSeatInfo.getCards())) {
                blackJackPlayerInfo.cardInfos = getCardInfos(playerSeatInfo, controller);
            }
        }
        blackJackPlayerInfo.pokerPlayerInfo = pokerPlayerInfo;
        return blackJackPlayerInfo;
    }

    public static List<BlackJackCardInfo> getBlackJackCardInfoList(BlackJackGameDataVo gameDataVo) {
        List<BlackJackCardInfo> blackJackCardInfos = new ArrayList<>();
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            if (playerSeatInfo.isDelState()) {
                continue;
            }
            BlackJackCardInfo blackJackCardInfo = new BlackJackCardInfo();
            blackJackCardInfo.playerId = playerSeatInfo.getPlayerId();
            blackJackCardInfo.cardIds = BlackJackDataHelper.getClientId(gameDataVo, playerSeatInfo.getCurrentCards());
            Map<Integer, Long> betInfo = gameDataVo.getAllBetInfo().get(playerSeatInfo.getPlayerId());
            if (Objects.nonNull(betInfo)) {
                blackJackCardInfo.betValue = betInfo.values().stream().mapToLong(Long::longValue).sum();
            }
            //计算总点数
            blackJackCardInfo.totalPoint = BlackJackDataHelper.getShowTotalPoint(playerSeatInfo.getCurrentCards());
            blackJackCardInfos.add(blackJackCardInfo);
        }
        return blackJackCardInfos;
    }

    /**
     * 获取21点手牌详细信息
     */
    public static List<BlackJackCardInfo> getCardInfos(PlayerSeatInfo playerSeatInfo, BlackJackGameController controller) {
        BlackJackGameDataVo gameDataVo = controller.getGameDataVo();
        List<BlackJackCardInfo> cardInfos = new ArrayList<>();
        for (int i = 0; i < playerSeatInfo.getCards().size(); i++) {
            List<Integer> card = playerSeatInfo.getCards().get(i);
            BlackJackCardInfo blackJackCardInfo = new BlackJackCardInfo();
            blackJackCardInfo.playerId = playerSeatInfo.getPlayerId();
            blackJackCardInfo.cardIds = BlackJackDataHelper.getClientId(gameDataVo, card);
            blackJackCardInfo.totalPoint = BlackJackDataHelper.getShowTotalPoint(card);
            blackJackCardInfo.betValue = controller.getPlayerSingleTotalBet(playerSeatInfo.getPlayerId(), i);
            cardInfos.add(blackJackCardInfo);
        }
        return cardInfos;
    }

    public static NotifyBlackJackBetResult buildNotifyBlackJackBetResult(int type, long betValue, long playerId, long totalBet) {
        NotifyBlackJackBetResult betResult = new NotifyBlackJackBetResult();
        betResult.type = type;
        betResult.betValue = betValue;
        betResult.playerId = playerId;
        betResult.totalBetValue = totalBet;
        return betResult;
    }
}

