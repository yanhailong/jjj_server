package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.data.TexasSaveHistory;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryPlayerInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasPotInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasRoundInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasSettlementPlayerInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasAllInSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSettlementPlayerChange;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.poker.game.texas.util.PlayerHand;
import com.jjg.game.poker.game.texas.util.PokerHandEvaluator;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.texas.constant.TexasConstant.Common.*;

/**
 * @author lm
 * @date 2025/7/29 09:31
 */
public class TexasSettlementPhase extends BaseSettlementPhase<TexasGameDataVo> {
    public TexasSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, TexasGameDataVo> gameController) {
        super(gameController);
    }
    @Override
    public int getPhaseRunTime() {
        //全all 计算需要增加的时间
        if (gameDataVo.getSettlement() == FLIP_CARDS_ROUND) {
            int remainRound = MAX_ROUND - gameDataVo.getRound();
            int addTimes = 0;
            for (int i = 0; i < remainRound; i++) {
                if (Objects.isNull(gameDataVo.getPublicCards()) || gameDataVo.getPublicCards().isEmpty()) {
                    addTimes += SEND_CARD_NUM;
                } else {
                    addTimes += ADD_CARDS;
                }
            }
            return super.getPhaseRunTime() + TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS) * addTimes;
        }
        return super.getPhaseRunTime();
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (gameController instanceof TexasGameController controller) {
            switch (gameDataVo.getSettlement()) {
                case DISCARD_SETTLEMENT -> settlementByOnePlayer(controller);
                case ALL_SETTLEMENT -> settlementByAllIn(controller);
                default -> normalSettlement(controller);
            }
        }
    }

    /**
     * 正常结算
     */
    private void normalSettlement(TexasGameController controller) {
        NotifyTexasSettlementInfo notifyTexasSettlementInfo = getNormalSettlementInfo(controller);
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasSettlementInfo));
        gameDataVo.setNotifyTexasSettlementInfo(notifyTexasSettlementInfo);
    }

    /**
     * 获取正常结算的结算信息
     */
    private NotifyTexasSettlementInfo getNormalSettlementInfo(TexasGameController controller) {
        List<Pot> pool = gameDataVo.getPool();
        //玩家获得 玩家id->获得金币
        Map<Long, Long> playerGet = new HashMap<>();
        //获胜的玩家牌型 玩家id->手牌结果
        Map<Long, HandResult> playerFinalCards = new HashMap<>();
        //摊牌的玩家信息 玩家id-> 玩家信息->手牌
        Map<Long, Pair<PlayerSeatInfo, List<Card>>> playerCards = new HashMap<>();
        Map<Integer, PokerCard> cardListMap = TexasDataHelper.getCardListMap(TexasDataHelper.getPoolId(gameDataVo));
        List<Card> publicCards = gameDataVo.getPublicCards().stream().map(cardListMap::get).collect(Collectors.toList());
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getOperationType() == PokerConstant.PlayerOperation.DISCARD) {
                continue;
            }
            List<Card> cards = info.getCurrentCards().stream().map(cardListMap::get).collect(Collectors.toList());
            playerCards.put(info.getPlayerId(), Pair.newPair(info, cards));
        }
        List<TexasPotInfo> texasPotInfos = new ArrayList<>(pool.size());
        //取所有池子
        for (Pot pot : pool) {
            Set<Long> eligiblePlayers = pot.getEligiblePlayers();
            if (eligiblePlayers.isEmpty()) {
                continue;
            }
            //获取牌比大小
            List<PlayerHand> hands = new ArrayList<>();
            for (Long eligiblePlayer : eligiblePlayers) {
                Pair<PlayerSeatInfo, List<Card>> pair = playerCards.get(eligiblePlayer);
                if (Objects.isNull(pair)) {
                    continue;
                }
                hands.add(new PlayerHand(eligiblePlayer, pair.getSecond()));
            }
            if (hands.isEmpty()) {
                continue;
            }
            //获胜的玩家信息
            List<Pair<Long, HandResult>> winners = PokerHandEvaluator.findWinners(hands, publicCards);
            //直接当抽水
            long remaining = pot.getAmount() % winners.size();
            //计算每个人获得的钱
            long avg = (pot.getAmount() - remaining) / winners.size();
            List<Long> playerPotInfos = new ArrayList<>(winners.size());
            for (Pair<Long, HandResult> winner : winners) {
                playerPotInfos.add(winner.getFirst());
                playerGet.merge(winner.getFirst(), avg, Long::sum);
                playerFinalCards.put(winner.getFirst(), winner.getSecond());
            }
            TexasPotInfo texasPotInfo = new TexasPotInfo();
            texasPotInfo.playerIdPotInfos = playerPotInfos;
            texasPotInfos.add(texasPotInfo);
        }
        //生成记录
        TexasSaveHistory texasHistory = gameDataVo.getTexasHistory();
        Map<Long, List<Integer>> settlementAllCards = new HashMap<>();
        for (TexasHistoryPlayerInfo info : texasHistory.getTotalPlayerBetInfo()) {
            info.betValue = playerGet.getOrDefault(info.playerId, 0L) - info.betValue;
        }
        texasHistory.setSettlementAllCards(settlementAllCards);
        texasHistory.setPotList(gameDataVo.getPool().stream().map(Pot::getAmount).collect(Collectors.toList()));
        //通知结算
        NotifyTexasSettlementInfo notifyTexasSettlementInfo = new NotifyTexasSettlementInfo();
        notifyTexasSettlementInfo.potInfos = texasPotInfos;
        notifyTexasSettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        List<TexasSettlementPlayerInfo> settlementInfoArrayList = new ArrayList<>();
        //生成结算信息 并发奖
        for (Map.Entry<Long, Pair<PlayerSeatInfo, List<Card>>> entry : playerCards.entrySet()) {
            Long playerId = entry.getKey();
            Pair<PlayerSeatInfo, List<Card>> pair = entry.getValue();
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
            TexasSettlementPlayerInfo settlementPlayerInfo = new TexasSettlementPlayerInfo();
            PokerPlayerSettlementInfo pokerPlayerSettlementInfo = new PokerPlayerSettlementInfo();
            settlementPlayerInfo.pokerPlayerSettlementInfo = pokerPlayerSettlementInfo;
            pokerPlayerSettlementInfo.playerId = playerId;
            HandResult handResult = playerFinalCards.get(playerId);
            if (Objects.isNull(handResult)) {
                handResult = TexasBuilder.getTempHandType(pair.getFirst(), gameDataVo);
            }
            settlementPlayerInfo.cards = handResult.getBestCards().stream()
                    .map(card -> ((PokerCard) card).getClientId()).collect(Collectors.toList());
            //添加记录
            settlementAllCards.put(playerId, settlementPlayerInfo.cards);
            long get = playerGet.getOrDefault(playerId, 0L) - baseBetInfo.getOrDefault(playerId, 0L);
            if (get > 0) {
                //扣税
                get = get * (10000 - gameDataVo.getRoomCfg().getEffectiveRatio()) / 10000;
                //增加金币
                controller.changePlayerGold(gamePlayer, get);
            }
            //添加记录
            pokerPlayerSettlementInfo.currentGold = gamePlayer.getGold();
            pokerPlayerSettlementInfo.getGold = get;
            pokerPlayerSettlementInfo.win = pokerPlayerSettlementInfo.getGold > 0;
            settlementPlayerInfo.cardType = handResult.getHandRank().rank;
            settlementPlayerInfo.handCards = PokerDataHelper.getClientId(pair.getFirst().getCurrentCards(), TexasDataHelper.getPoolId(gameDataVo));
            settlementInfoArrayList.add(settlementPlayerInfo);
        }
        notifyTexasSettlementInfo.playerSettlementInfos = settlementInfoArrayList;
        return notifyTexasSettlementInfo;
    }

    /**
     * 在非最后阶段全部all
     */
    public void settlementByAllIn(TexasGameController controller) {
        //获取当前阶段
        int round = gameDataVo.getRound();
        int remainingRounds = TexasConstant.Common.MAX_ROUND - round;
        //发牌
        int addTime = 0;
        Map<Long, List<TexasRoundInfo>> playerRoundInfos = new HashMap<>();
        List<TexasRoundInfo> defaultInfo = new ArrayList<>();
        for (int i = 0; i < remainingRounds; i++) {
            int nextRound = round + i + 1;
            List<Integer> tempCardList;
            List<Integer> publicCards = gameDataVo.getPublicCards();
            if (Objects.isNull(publicCards)) {
                //还没发三张
                addTime += TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS) * SEND_CARD_NUM;
                List<Integer> cardList = gameDataVo.getCards().subList(0, SEND_CARD_NUM);
                gameDataVo.setPublicCards(new ArrayList<>(cardList));
                tempCardList = new ArrayList<>(cardList);
                cardList.clear();
            } else {
                //发一张
                Integer card = gameDataVo.getCards().remove(0);
                gameDataVo.getPublicCards().add(card);
                tempCardList = List.of(card);
                addTime += TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
            }
            List<Integer> clientId = TexasDataHelper.getClientId(tempCardList, TexasDataHelper.getPoolId(gameDataVo));
            for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                List<TexasRoundInfo> texasRoundInfos = playerRoundInfos.computeIfAbsent(info.getPlayerId(), k -> new ArrayList<>());
                int rank = TexasBuilder.getTempHandType(info, gameDataVo).getHandRank().rank;
                texasRoundInfos.add(TexasBuilder.getTexasRoundInfo(nextRound, clientId, rank));
            }
            TexasRoundInfo texasRoundInfo = new TexasRoundInfo();
            texasRoundInfo.round = round;
            texasRoundInfo.cards = clientId;
            defaultInfo.add(texasRoundInfo);
        }
        NotifyTexasSettlementInfo normalSettlementInfo = getNormalSettlementInfo(controller);
        //计算最后的结算时间
        gameDataVo.setPhaseEndTime(gameDataVo.getPhaseEndTime() + addTime);
        normalSettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        gameDataVo.setNotifyTexasSettlementInfo(normalSettlementInfo);
        //通知
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            List<TexasRoundInfo> orDefault = playerRoundInfos.getOrDefault(seatInfo.getPlayerId(), defaultInfo);
            NotifyTexasAllInSettlementInfo inSettlementInfo = TexasBuilder.getNotifyAllInSettlementInfo(normalSettlementInfo, orDefault);
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(seatInfo.getPlayerId(), inSettlementInfo));
        }
    }

    /**
     * 弃牌到剩一个人结算
     */
    public void settlementByOnePlayer(TexasGameController controller) {
        List<PlayerSeatInfo> infoList = gameDataVo.getPlayerSeatInfoList()
                .stream()
                .filter(info -> info.getOperationType() != PokerConstant.PlayerOperation.DISCARD)
                .toList();
        if (infoList.size() > 1) {
            log.error("出现错误 未弃牌人数大于1");
            normalSettlement(controller);
            return;
        }
        long playerId = infoList.get(0).getPlayerId();
        long total = 0;
        for (Pot pot : gameDataVo.getPool()) {
            total += pot.getAmount();
        }
        NotifyTexasSettlementInfo notifyTexasSettlementInfo = new NotifyTexasSettlementInfo();
        notifyTexasSettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        List<TexasSettlementPlayerInfo> settlementInfoArrayList = new ArrayList<>();
        //广播
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        PokerPlayerSettlementInfo pokerPlayerSettlementInfo = new PokerPlayerSettlementInfo();
        pokerPlayerSettlementInfo.playerId = playerId;
        //增加金币
        long get = total - baseBetInfo.getOrDefault(playerId, 0L);
        //添加记录
        TexasSaveHistory texasHistory = gameDataVo.getTexasHistory();
        for (TexasHistoryPlayerInfo info : texasHistory.getTotalPlayerBetInfo()) {
            if (info.playerId == playerId) {
                info.betValue = get;
                continue;
            }
            info.betValue = -info.betValue;
        }
        get = get * (10000 - gameDataVo.getRoomCfg().getEffectiveRatio()) / 10000;
        controller.changePlayerGold(gamePlayer, total);
        pokerPlayerSettlementInfo.currentGold = gamePlayer.getGold();
        pokerPlayerSettlementInfo.getGold = get;
        pokerPlayerSettlementInfo.win = pokerPlayerSettlementInfo.getGold > 0;
        TexasSettlementPlayerInfo settlementPlayerInfo = new TexasSettlementPlayerInfo();
        settlementPlayerInfo.pokerPlayerSettlementInfo = pokerPlayerSettlementInfo;
        settlementInfoArrayList.add(settlementPlayerInfo);
        notifyTexasSettlementInfo.playerSettlementInfos = settlementInfoArrayList;
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasSettlementInfo));
        gameDataVo.setNotifyTexasSettlementInfo(notifyTexasSettlementInfo);
    }


    @Override
    public void phaseFinishDoAction() {
        if (gameController instanceof BasePokerGameController<TexasGameDataVo> controller) {
            //设置为等待阶段
            controller.setCurrentGamePhase(new BaseWaitReadyPhase<>(gameController));
            gameDataVo.getTexasHistoryList().add(gameDataVo.getTexasHistory());
            //金币不够底注的尝试重新拿金币
            updatePlayerData();
        }
    }

    /**
     * 结算后更新玩家信息
     */
    public void updatePlayerData() {
        if (gameController instanceof TexasGameController controller) {
            List<PokerPlayerInfo> infos = new ArrayList<>();
            for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
                seatInfo.setJoinGame(false);
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
                if (Objects.isNull(gamePlayer)) {
                    continue;
                }
                controller.addTempGoldOrOutTable(seatInfo, gamePlayer);
                infos.add(PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, gameDataVo));
            }
            NotifyTexasSettlementPlayerChange notifySettlementPlayerChange = new NotifyTexasSettlementPlayerChange();
            notifySettlementPlayerChange.pokerPlayerInfos = infos;
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifySettlementPlayerChange));
        }
    }

}
