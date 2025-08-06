package com.jjg.game.poker.game.texas.message;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.message.reps.NotifySettlementInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasRoundInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyAllInSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyPublicCardChange;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.poker.game.texas.util.PokerHandEvaluator;

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

    public static NotifyPublicCardChange getNotifyPublicCardChange(PlayerSeatInfo playerSeatInfo, PlayerSeatInfo nextExePlayer, List<Integer> addCards, TexasGameDataVo gameDataVo) {
        NotifyPublicCardChange notifyPublicCardChange = new NotifyPublicCardChange();
        TexasRoundInfo texasRoundInfo = new TexasRoundInfo();
        texasRoundInfo.cards = TexasDataHelper.getClientId(addCards,gameDataVo.getRoomCfg().getId());
        texasRoundInfo.round = gameDataVo.getRound();
        if (Objects.nonNull(playerSeatInfo)) {
            texasRoundInfo.handType = getTempHandType(playerSeatInfo, gameDataVo).getHandRank().rank;
        }
        notifyPublicCardChange.roundInfo = texasRoundInfo;
        notifyPublicCardChange.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        notifyPublicCardChange.playerId = nextExePlayer.getPlayerId();
        return notifyPublicCardChange;
    }

    public static HandResult getTempHandType(PlayerSeatInfo info, TexasGameDataVo gameDataVo) {
        List<Integer> publicCards = new ArrayList<>(gameDataVo.getPublicCards());
        publicCards.addAll(info.getCurrentCards());
        Map<Integer, PokerCard> cardListMap = TexasDataHelper.getCardListMap(gameDataVo.getRoomCfg().getId());
        return PokerHandEvaluator.evaluateBestHand(publicCards.stream().map(cardListMap::get).collect(Collectors.toList()));
    }

    public static TexasRoundInfo getTexasRoundInfo(int round, List<Integer> cards,int handRank) {
        TexasRoundInfo texasRoundInfo = new TexasRoundInfo();
        texasRoundInfo.cards = cards;
        texasRoundInfo.round = round;
        texasRoundInfo.handType = handRank;
        return texasRoundInfo;
    }

    public static NotifyAllInSettlementInfo getNotifyAllInSettlementInfo(NotifySettlementInfo notifySettlementInfo, List<TexasRoundInfo> texasRoundInfos) {
        NotifyAllInSettlementInfo notifyAllInSettlementInfo = new NotifyAllInSettlementInfo();
        notifyAllInSettlementInfo.settlementInfo = notifySettlementInfo;
        notifyAllInSettlementInfo.roundInfos = texasRoundInfos;
        return  notifyAllInSettlementInfo;
    }
}
