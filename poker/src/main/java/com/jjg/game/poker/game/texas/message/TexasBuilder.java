package com.jjg.game.poker.game.texas.message;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryPlayerInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasPlayerInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasRoundInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasAllInSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasPublicCardChange;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSettlementInfo;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.poker.game.texas.util.PokerHandEvaluator;
import com.jjg.game.room.data.room.GamePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(TexasBuilder.class);

    public static NotifyTexasPublicCardChange getNotifyPublicCardChange(PlayerSeatInfo playerSeatInfo, PlayerSeatInfo nextExePlayer, List<Integer> addCards, TexasGameDataVo gameDataVo) {
        NotifyTexasPublicCardChange notifyTexasPublicCardChange = new NotifyTexasPublicCardChange();
        TexasRoundInfo texasRoundInfo = new TexasRoundInfo();
        texasRoundInfo.cards = TexasDataHelper.getClientId(gameDataVo, addCards);
        texasRoundInfo.round = gameDataVo.getRound();
        if (Objects.nonNull(playerSeatInfo)) {
            HandResult tempHandType = getTempHandType(playerSeatInfo, gameDataVo);
            if (Objects.nonNull(tempHandType)) {
                texasRoundInfo.handType = tempHandType.getHandRank().rank;
            }
        }
        notifyTexasPublicCardChange.potList = gameDataVo.getPotValueList();
        notifyTexasPublicCardChange.roundInfo = texasRoundInfo;
        notifyTexasPublicCardChange.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
        notifyTexasPublicCardChange.playerId = nextExePlayer.getPlayerId();
        return notifyTexasPublicCardChange;
    }

    public static HandResult getTempHandType(PlayerSeatInfo info, TexasGameDataVo gameDataVo) {
        List<Integer> publicCards;
        if (gameDataVo.getPublicCards() == null) {
            publicCards = new ArrayList<>(info.getCurrentCards());
        } else {
            publicCards = new ArrayList<>(gameDataVo.getPublicCards());
            publicCards.addAll(info.getCurrentCards());
        }
        Map<Integer, PokerCard> cardListMap = TexasDataHelper.getCardListMap(TexasDataHelper.getPoolId(gameDataVo));
        return PokerHandEvaluator.evaluateBestHand(publicCards.stream().map(cardListMap::get).collect(Collectors.toList()));
    }

    public static HandResult getRobotTempHandType(PlayerSeatInfo info, TexasGameDataVo gameDataVo) {
        List<Integer> oldPublicCards = gameDataVo.getPublicCards();
        List<Integer> publicCards = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(oldPublicCards)) {
            publicCards.addAll(oldPublicCards);
        }
        int needFind = 5 - publicCards.size();
        for (int j = 0; j < needFind; j++) {
            List<Integer> cards = gameDataVo.getCards();
            publicCards.add(cards.get(j));
        }
        publicCards.addAll(info.getCurrentCards());
        log.info("roomId:{} playerId:{} 德州机器人获取牌型 公牌id:{}", gameDataVo.getRoomId(), info.getPlayerId(), publicCards);
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
        return getTexasHistoryPlayerInfo(info, texasGameDataVo, false, betValue);
    }

    public static TexasHistoryPlayerInfo getTexasHistoryPlayerInfo(TexasHistoryPlayerInfo info, Map<Long, List<Integer>> allCards) {
        TexasHistoryPlayerInfo texasHistoryPlayerInfo = new TexasHistoryPlayerInfo();
        texasHistoryPlayerInfo.playerName = info.playerName;
        texasHistoryPlayerInfo.playerId = info.playerId;
        texasHistoryPlayerInfo.betValue = info.betValue;
        texasHistoryPlayerInfo.index = info.index;
        if (Objects.nonNull(allCards)) {
            texasHistoryPlayerInfo.cardIds = allCards.get(info.playerId);
        }
        return texasHistoryPlayerInfo;
    }

    public static TexasHistoryPlayerInfo getTexasHistoryPlayerInfo(PlayerSeatInfo info, TexasGameDataVo texasGameDataVo, boolean init, long betValue) {
        texasGameDataVo.getRoundBet().merge(info.getPlayerId(), betValue, Long::sum);
        TexasHistoryPlayerInfo texasHistoryPlayerInfo = new TexasHistoryPlayerInfo();
        texasHistoryPlayerInfo.betValue = betValue;
        texasHistoryPlayerInfo.playerId = info.getPlayerId();
        //获取位置
        int dealerIndex = texasGameDataVo.getDealerIndex();
        if (init) {
            List<PlayerSeatInfo> playerSeatInfoList = texasGameDataVo.getPlayerSeatInfoList();
            for (int i = 0; i < playerSeatInfoList.size(); i++) {
                PlayerSeatInfo seatInfo = playerSeatInfoList.get(i);
                if (seatInfo.getPlayerId() == info.getPlayerId()) {
                    if (i >= dealerIndex) {
                        texasHistoryPlayerInfo.index = i - dealerIndex;
                    } else {
                        texasHistoryPlayerInfo.index = playerSeatInfoList.size() - dealerIndex + i;
                    }
                    break;
                }
            }
        } else {
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


    public static TexasPlayerInfo getTexasPlayerInfo(PlayerSeatInfo playerSeatInfo, SeatInfo seatInfo, TexasGameController controller) {
        TexasPlayerInfo texasPlayerInfo = new TexasPlayerInfo();
        TexasGameDataVo gameDataVo = controller.getGameDataVo();
        texasPlayerInfo.totalBet = gameDataVo.getRoundBet().getOrDefault(seatInfo.getPlayerId(), 0L);
        PokerPlayerInfo playerInfo = PokerBuilder.getPokerPlayerInfo(seatInfo, controller);
        if (Objects.nonNull(playerSeatInfo) && !playerSeatInfo.isDelState()) {
            texasPlayerInfo.handCards = TexasDataHelper.getClientId(gameDataVo, playerSeatInfo.getCurrentCards());
            playerInfo.operationType = playerSeatInfo.getOperationType();
        }
        texasPlayerInfo.pokerPlayerInfo = playerInfo;
        return texasPlayerInfo;
    }
}
