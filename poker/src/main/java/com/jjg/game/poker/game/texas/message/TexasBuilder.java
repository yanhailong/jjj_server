package com.jjg.game.poker.game.texas.message;

import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryPlayerInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSettlementInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasRoundInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasAllInSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasPublicCardChange;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.poker.game.texas.util.PokerHandEvaluator;
import com.jjg.game.room.data.room.GamePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/7/31 14:38
 */
public class TexasBuilder {

    public static NotifyTexasPublicCardChange getNotifyPublicCardChange(PlayerSeatInfo playerSeatInfo, PlayerSeatInfo nextExePlayer, List<Integer> addCards, TexasGameDataVo gameDataVo) {
        NotifyTexasPublicCardChange notifyTexasPublicCardChange = new NotifyTexasPublicCardChange();
        TexasRoundInfo texasRoundInfo = new TexasRoundInfo();
        texasRoundInfo.cards = TexasDataHelper.getClientId(addCards, TexasDataHelper.getPoolId(gameDataVo));
        texasRoundInfo.round = gameDataVo.getRound();
        if (Objects.nonNull(playerSeatInfo)) {
            texasRoundInfo.handType = getTempHandType(playerSeatInfo, gameDataVo).getHandRank().rank;
        }
        notifyTexasPublicCardChange.roundInfo = texasRoundInfo;
        notifyTexasPublicCardChange.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        notifyTexasPublicCardChange.playerId = nextExePlayer.getPlayerId();
        return notifyTexasPublicCardChange;
    }

    public static HandResult getTempHandType(PlayerSeatInfo info, TexasGameDataVo gameDataVo) {
        if (Objects.isNull(gameDataVo.getPublicCards())) {
            return null;
        }
        List<Integer> publicCards = new ArrayList<>(gameDataVo.getPublicCards());
        publicCards.addAll(info.getCurrentCards());
        Map<Integer, PokerCard> cardListMap = TexasDataHelper.getCardListMap(TexasDataHelper.getPoolId(gameDataVo));
        return PokerHandEvaluator.evaluateBestHand(publicCards.stream().map(cardListMap::get).collect(Collectors.toList()));
    }

    public static TexasRoundInfo getTexasRoundInfo(int round, List<Integer> cards, int handRank) {
        TexasRoundInfo texasRoundInfo = new TexasRoundInfo();
        texasRoundInfo.cards = cards;
        texasRoundInfo.round = round;
        texasRoundInfo.handType = handRank;
        return texasRoundInfo;
    }

    public static NotifyTexasAllInSettlementInfo getNotifyAllInSettlementInfo(NotifyTexasSettlementInfo notifyTexasSettlementInfo, List<TexasRoundInfo> texasRoundInfos) {
        NotifyTexasAllInSettlementInfo notifyTexasAllInSettlementInfo = new NotifyTexasAllInSettlementInfo();
        notifyTexasAllInSettlementInfo.settlementInfo = notifyTexasSettlementInfo;
        notifyTexasAllInSettlementInfo.roundInfos = texasRoundInfos;
        return notifyTexasAllInSettlementInfo;
    }

    public static TexasHistoryPlayerInfo getTexasHistoryPlayerInfo(PlayerSeatInfo info, TexasGameDataVo texasGameDataVo, long betValue) {
        TexasHistoryPlayerInfo texasHistoryPlayerInfo = new TexasHistoryPlayerInfo();
        texasHistoryPlayerInfo.betValue = betValue;
        texasHistoryPlayerInfo.playerId = info.getPlayerId();
        //获取位置
        int dealerIndex = texasGameDataVo.getDealerIndex();
        List<TexasHistoryPlayerInfo> totalPlayerBetInfo = texasGameDataVo.getTexasHistory().getTotalPlayerBetInfo();
        for (int i = 0; i < totalPlayerBetInfo.size(); i++) {
            TexasHistoryPlayerInfo historyPlayerInfo = totalPlayerBetInfo.get(i);
            if (historyPlayerInfo.playerId == info.getPlayerId()) {
                if (i >= dealerIndex) {
                    texasHistoryPlayerInfo.index = i - dealerIndex;
                } else {
                    texasHistoryPlayerInfo.index = totalPlayerBetInfo.size() - dealerIndex + i;
                }
                break;
            }
        }
        GamePlayer gamePlayer = texasGameDataVo.getGamePlayer(info.getPlayerId());
        if (Objects.nonNull(gamePlayer)) {
            texasHistoryPlayerInfo.playerName = gamePlayer.getNickName();
        }
        if (info.getOperationType() != PokerConstant.PlayerOperation.NONE) {
            texasHistoryPlayerInfo.operationType = info.getOperationType();
        }
        return texasHistoryPlayerInfo;
    }
}
