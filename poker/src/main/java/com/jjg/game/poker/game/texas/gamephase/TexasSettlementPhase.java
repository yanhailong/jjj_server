package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.poker.game.common.message.bean.PlayerInfo;
import com.jjg.game.poker.game.common.message.bean.PlayerSettlementInfo;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.message.bean.TexasPotInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasRoundInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyAllInSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifySettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifySettlementPlayerChange;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.poker.game.texas.util.PlayerHand;
import com.jjg.game.poker.game.texas.util.PokerHandEvaluator;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/7/29 09:31
 */
public class TexasSettlementPhase extends BaseSettlementPhase<TexasGameDataVo> {


    public TexasSettlementPhase(AbstractGameController<Room_ChessCfg, TexasGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public int getPhaseRunTime() {
        //全all 计算需要增加的时间
        if (gameDataVo.getSettlement() == 2) {
            int remainRound = TexasConstant.Common.MAX_ROUND - gameDataVo.getRound();
            int addTimes = 0;
            for (int i = 0; i < remainRound; i++) {
                if (Objects.isNull(gameDataVo.getPublicCards()) || gameDataVo.getPublicCards().isEmpty()) {
                    addTimes += 3;
                } else {
                    addTimes += 1;
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
                case 1 -> settlementByOnePlayer(controller);
                case 2 -> settlementByAllIn(controller);
                default -> normalSettlement(controller);
            }
        }
    }

    /**
     * 正常结算
     */
    private void normalSettlement(TexasGameController controller) {
        NotifySettlementInfo notifySettlementInfo = getNormalSettlementInfo(controller);
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifySettlementInfo));
        gameDataVo.setNotifySettlementInfo(notifySettlementInfo);
    }

    private NotifySettlementInfo getNormalSettlementInfo(TexasGameController controller) {
        List<Pot> pool = gameDataVo.getPool();
        //用奖池发奖
        Map<Long, Long> playerGet = new HashMap<>();
        Map<Long, HandResult> playerFinalCards = new HashMap<>();
        Map<Long, Pair<PlayerSeatInfo, List<Card>>> playerCards = new HashMap<>();
        Map<Integer, PokerCard> cardListMap = TexasDataHelper.getCardListMap(gameDataVo.getRoomCfg().getId());
        List<Card> publicCards = gameDataVo.getPublicCards().stream().map(cardListMap::get).collect(Collectors.toList());
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getOperationType() == PokerConstant.PlayerOperation.DISCARD) {
                continue;
            }
            List<Card> cards = info.getCurrentCards().stream().map(cardListMap::get).collect(Collectors.toList());
            playerCards.put(info.getPlayerId(), Pair.newPair(info, cards));
        }
        List<TexasPotInfo> texasPotInfos = new ArrayList<>(pool.size());
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
        //通知结算
        NotifySettlementInfo notifySettlementInfo = new NotifySettlementInfo();
        notifySettlementInfo.potInfos = texasPotInfos;
        notifySettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        List<PlayerSettlementInfo> settlementInfoArrayList = new ArrayList<>();
        //广播
        for (Map.Entry<Long, Pair<PlayerSeatInfo, List<Card>>> entry : playerCards.entrySet()) {
            Long playerId = entry.getKey();
            Pair<PlayerSeatInfo, List<Card>> pair = entry.getValue();
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);

            PlayerSettlementInfo playerSettlementInfo = new PlayerSettlementInfo();
            playerSettlementInfo.playerId = playerId;
            HandResult handResult = playerFinalCards.get(playerId);
            if (Objects.isNull(handResult)) {
                handResult = TexasBuilder.getTempHandType(pair.getFirst(), gameDataVo);
            }
            playerSettlementInfo.cards = handResult.getBestCards().stream()
                    .map(card -> ((PokerCard) card).getClientId()).collect(Collectors.toList());
            long get = playerGet.getOrDefault(playerId, 0L) - baseBetInfo.getOrDefault(playerId, 0L);
            if (get > 0) {
                //扣税
                get = get * (10000 - gameDataVo.getRoomCfg().getEffectiveRatio()) / 10000;
                //增加金币
                controller.changePlayerGold(gamePlayer, get);
            }
            playerSettlementInfo.currentGold = gamePlayer.getGold();
            playerSettlementInfo.getGold = get;
            playerSettlementInfo.win = playerSettlementInfo.getGold > 0;
            playerSettlementInfo.cardType = handResult.getHandRank().rank;
            settlementInfoArrayList.add(playerSettlementInfo);
        }
        notifySettlementInfo.playerSettlementInfos = settlementInfoArrayList;
        return notifySettlementInfo;
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
                addTime += TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS) * 3;
                List<Integer> cardList = gameDataVo.getCards().subList(0, 3);
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
            List<Integer> clientId = TexasDataHelper.getClientId(tempCardList, gameDataVo.getRoomCfg().getId());
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
        NotifySettlementInfo normalSettlementInfo = getNormalSettlementInfo(controller);
        //计算最后的结算时间
        gameDataVo.setPhaseEndTime(gameDataVo.getPhaseEndTime() + addTime);
        normalSettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        gameDataVo.setNotifySettlementInfo(normalSettlementInfo);
        //通知
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            List<TexasRoundInfo> orDefault = playerRoundInfos.getOrDefault(seatInfo.getPlayerId(), defaultInfo);
            NotifyAllInSettlementInfo inSettlementInfo = TexasBuilder.getNotifyAllInSettlementInfo(normalSettlementInfo, orDefault);
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(seatInfo.getPlayerId(), inSettlementInfo));
        }
    }

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
        NotifySettlementInfo notifySettlementInfo = new NotifySettlementInfo();
        notifySettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        List<PlayerSettlementInfo> settlementInfoArrayList = new ArrayList<>();
        //广播
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        PlayerSettlementInfo playerSettlementInfo = new PlayerSettlementInfo();
        playerSettlementInfo.playerId = playerId;
        //增加金币
        long get = total - baseBetInfo.getOrDefault(playerId, 0L);
        get = get * (10000 - gameDataVo.getRoomCfg().getEffectiveRatio()) / 10000;
        controller.changePlayerGold(gamePlayer, total);
        playerSettlementInfo.currentGold = gamePlayer.getGold();
        playerSettlementInfo.getGold = get;
        playerSettlementInfo.win = playerSettlementInfo.getGold > 0;
        settlementInfoArrayList.add(playerSettlementInfo);
        notifySettlementInfo.playerSettlementInfos = settlementInfoArrayList;
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifySettlementInfo));
        gameDataVo.setNotifySettlementInfo(notifySettlementInfo);
    }

    @Override
    public void phaseFinish() {
        //设置为等待阶段
        gameController.setCurrentGamePhase(new BaseWaitReadyPhase<>(gameController));
        gameDataVo.addId();
        //金币不够底注的尝试重新拿金币
        updatePlayerData();
        //开启下一局
        if (gameController instanceof TexasGameController controller) {
            controller.tryStartGame();
        }
    }

    public void updatePlayerData() {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        List<PlayerInfo> infos = new ArrayList<>();
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            seatInfo.setJoinGame(false);
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
            if (Objects.isNull(gamePlayer)) {
                continue;
            }
            //TODO 配置
            Long hasTempCurrency = gameDataVo.getTempGold().getOrDefault(seatInfo.getPlayerId(), 0L);
            if (hasTempCurrency < roomCfg.getBetBase()) {
                long defaultCoinsNum = TexasDataHelper.getDefaultCoinsNum(gameDataVo);
                if (gamePlayer.getGold() >= defaultCoinsNum) {
                    //增加零时货币
                    gameDataVo.getTempGold().put(seatInfo.getPlayerId(), defaultCoinsNum);
                } else {
                    //自动下桌
                    seatInfo.setSeatDown(false);
                }
            }
            infos.add(PokerBuilder.buildPlayerInfo(gamePlayer, gameDataVo, false));
        }
        NotifySettlementPlayerChange notifySettlementPlayerChange = new NotifySettlementPlayerChange();
        notifySettlementPlayerChange.playerInfos = infos;
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifySettlementPlayerChange));
    }

}
