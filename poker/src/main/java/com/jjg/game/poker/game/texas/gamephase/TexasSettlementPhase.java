package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.base.gameevent.PlayerEventCategory.PlayerEffectiveFlowingEvent;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.task.param.TaskConditionParam12001;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;
import com.jjg.game.poker.game.texas.autohandler.TexasRobotHandler;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.data.TexasSaveHistory;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.message.bean.*;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasAllInSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSettlementInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSettlementPlayerChange;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.poker.game.texas.util.PlayerHand;
import com.jjg.game.poker.game.texas.util.PokerHandEvaluator;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.data.room.SimplePlayerInfo;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.robot.RobotUtil;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        //计算边池
        if (gameController instanceof TexasGameController controller && controller.hasAllIn()) {
            gameDataVo.setPool(TexasGameController.buildPots(gameDataVo));
        }
        //是否需要收筹码的时间
        int fixChips = gameDataVo.getMaxBetValue() > 0 ? FIX_CHIPS : 0;
        //全all 计算需要增加的时间
        if (gameDataVo.getSettlement() == ALL_SETTLEMENT) {
            int remainRound = MAX_ROUND - gameDataVo.getRound();
            int addTimes = 0;
            for (int i = 0; i < remainRound; i++) {
                if (Objects.isNull(gameDataVo.getPublicCards()) || gameDataVo.getPublicCards().isEmpty()) {
                    addTimes += SEND_CARD_NUM;
                } else {
                    addTimes += ADD_CARDS;
                }
            }
            return super.getPhaseRunTime() + remainRound * (FIX_CHIPS) + FLIP_CARDS +
                    TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS) * addTimes
                    + TexasDataHelper.getExecutionTime(gameDataVo, PokerPhase.SIDE_POOL) * gameDataVo.getPool().size();
        }
        return super.getPhaseRunTime() + fixChips + FLIP_CARDS + TexasDataHelper.getExecutionTime(gameDataVo,
                PokerPhase.SIDE_POOL) * gameDataVo.getPool().size();
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        try {
            if (gameController instanceof TexasGameController controller) {
                switch (gameDataVo.getSettlement()) {
                    case DISCARD_SETTLEMENT -> settlementByOnePlayer(controller);
                    case ALL_SETTLEMENT -> settlementByAllIn(controller);
                    default -> normalSettlement(controller);
                }
                addLog(controller, gameDataVo.getTexasHistory());
                //机器人处理
                List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
                for (PlayerSeatInfo playerSeatInfo : playerSeatInfoList) {
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerSeatInfo.getPlayerId());
                    if (gamePlayer instanceof GameRobotPlayer robotPlayer && playerSeatInfo.getOperationType() == PokerConstant.PlayerOperation.DISCARD) {
                        int chessExecutionDelay = RobotUtil.getChessExecutionDelay(robotPlayer.getActionId());
                        TexasRobotHandler handler = new TexasRobotHandler(robotPlayer, TexasRobotHandler.SHOW_CARDS, controller);
                        RobotUtil.schedule(gameController.getRoomController(), handler, chessExecutionDelay);
                    }
                }
            }
        } catch (Exception e) {
            log.error("德州扑克结算异常", e);
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
        List<Card> publicCards =
                gameDataVo.getPublicCards().stream().map(cardListMap::get).collect(Collectors.toList());
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getOperationType() == PokerConstant.PlayerOperation.DISCARD || info.isDelState()) {
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
        notifyTexasSettlementInfo.potList = gameDataVo.getPotValueList();
        notifyTexasSettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        List<TexasSettlementPlayerInfo> settlementInfoArrayList = new ArrayList<>();
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
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
            long totalGet = playerGet.getOrDefault(playerId, 0L);
            if (totalGet > 0) {
                //扣税
                Long bet = baseBetInfo.getOrDefault(playerId, 0L);
                BigDecimal tempTotalGet = BigDecimal.valueOf(totalGet - bet);
                long afterRatio = tempTotalGet.multiply(BigDecimal.valueOf((10000 - gameDataVo.getRoomCfg().getEffectiveRatio())))
                        .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();
                totalGet = bet + afterRatio;
                //增加金币
                controller.changePlayerGold(gamePlayer, totalGet);
                // 添加账单记录
                settlementDataMap.put(
                        playerId, settlementDataMap.getOrDefault(playerId, new SettlementData())
                                .increaseBySettlementData(new SettlementData(
                                        afterRatio, bet, totalGet, bet, tempTotalGet.longValue() - afterRatio)));
                if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                    robotPlayer.setLastWin(1);
                }
            } else {
                if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                    robotPlayer.setLastWin(2);
                }
            }
            pokerPlayerSettlementInfo.currentGold = gameDataVo.getTempGold().getOrDefault(playerId, 0L);
            pokerPlayerSettlementInfo.getGold = totalGet;
            pokerPlayerSettlementInfo.win = pokerPlayerSettlementInfo.getGold > 0;
            settlementPlayerInfo.cardType = handResult.getHandRank().rank;
            settlementPlayerInfo.handCards = TexasDataHelper.getClientId(gameDataVo, pair.getFirst().getCurrentCards());
            settlementInfoArrayList.add(settlementPlayerInfo);
        }
        // 添加房主流水记录
        addCreateRecord(controller, settlementDataMap);
        notifyTexasSettlementInfo.playerSettlementInfos = settlementInfoArrayList;
        return notifyTexasSettlementInfo;
    }

    /**
     * 添加房主记录
     * @param controller 游戏控制器
     * @param settlementDataMap 结算数据
     */
    private void addCreateRecord(TexasGameController controller, Map<Long, SettlementData> settlementDataMap) {
        if (gameController.getRoom() instanceof FriendRoom) {
            RoomBankerChangeParam roomBankerChangeParam = new RoomBankerChangeParam();
            long sum = settlementDataMap.values().stream().mapToLong(SettlementData::getTaxation).sum();
            roomBankerChangeParam.addRoomCreatorTotalIncome(calcRoomCreatorIncome(sum));
            controller.dealBankerFlowing(roomBankerChangeParam, settlementDataMap);
        }
    }

    /**
     * 在非最后阶段全部all
     */
    public void settlementByAllIn(TexasGameController controller) {
        //获取当前阶段
        int round = gameDataVo.getRound();
        int remainingRounds = TexasConstant.Common.MAX_ROUND - round;
        //发牌
        Map<Long, List<TexasRoundInfo>> playerRoundInfos = new HashMap<>();
        List<TexasRoundInfo> defaultInfo = new ArrayList<>();
        //添加记录
        TexasSaveHistory texasHistory = gameDataVo.getTexasHistory();
        List<Long> potAllBet = gameDataVo.getPool().stream().map(Pot::getAmount).collect(Collectors.toList());
        for (int i = 0; i < remainingRounds; i++) {
            int nextRound = round + i + 1;
            TexasHistoryRoundInfo texasHistoryRoundInfo = new TexasHistoryRoundInfo(nextRound);
            texasHistoryRoundInfo.potAllBet = potAllBet;
            texasHistory.getTexasHistoryRoundInfos().add(texasHistoryRoundInfo);
            List<Integer> tempCardList;
            List<Integer> publicCards = gameDataVo.getPublicCards();
            if (Objects.isNull(publicCards)) {
                //还没发三张
                List<Integer> cardList = gameDataVo.getCards().subList(0, SEND_CARD_NUM);
                gameDataVo.setPublicCards(new ArrayList<>(cardList));
                texasHistory.setPreFlop(TexasDataHelper.getClientId(gameDataVo, gameDataVo.getPublicCards()));
                tempCardList = new ArrayList<>(cardList);
                cardList.clear();
            } else {
                //发一张
                Integer card = gameDataVo.getCards().removeFirst();
                gameDataVo.getPublicCards().add(card);
                if (texasHistory.getThirdCardId() == 0) {
                    texasHistory.setThirdCardId(TexasDataHelper.getClientCardId(gameDataVo, card));
                } else if (texasHistory.getFourthCardId() == 0) {
                    texasHistory.setFourthCardId(TexasDataHelper.getClientCardId(gameDataVo, card));
                }
                tempCardList = List.of(card);
            }
            List<Integer> clientId = TexasDataHelper.getClientId(gameDataVo, tempCardList);
            for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                if (info.isDelState()) {
                    continue;
                }
                List<TexasRoundInfo> texasRoundInfos = playerRoundInfos.computeIfAbsent(info.getPlayerId(),
                        k -> new ArrayList<>());
                HandResult tempHandType = TexasBuilder.getTempHandType(info, gameDataVo);
                int rank = Objects.nonNull(tempHandType) ? tempHandType.getHandRank().rank : 0;
                texasRoundInfos.add(TexasBuilder.getTexasRoundInfo(nextRound, clientId, rank));
            }
            TexasRoundInfo texasRoundInfo = new TexasRoundInfo();
            texasRoundInfo.round = round;
            texasRoundInfo.cards = clientId;
            defaultInfo.add(texasRoundInfo);
        }
        NotifyTexasSettlementInfo normalSettlementInfo = getNormalSettlementInfo(controller);
        //计算最后的结算时间
        gameDataVo.setPhaseEndTime(gameDataVo.getPhaseEndTime());
        normalSettlementInfo.endTime = gameDataVo.getPhaseEndTime();
        gameDataVo.setNotifyTexasSettlementInfo(normalSettlementInfo);
        //通知
        for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
            List<TexasRoundInfo> orDefault = playerRoundInfos.getOrDefault(seatInfo.getPlayerId(), defaultInfo);
            NotifyTexasAllInSettlementInfo inSettlementInfo =
                    TexasBuilder.getNotifyAllInSettlementInfo(normalSettlementInfo, orDefault);
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(seatInfo.getPlayerId(),
                    inSettlementInfo));
        }
    }

    /**
     * 弃牌到剩一个人结算
     */
    public void settlementByOnePlayer(TexasGameController controller) {
        List<PlayerSeatInfo> infoList = gameDataVo.getPlayerSeatInfoList()
                .stream()
                .filter(info -> info.getOperationType() != PokerConstant.PlayerOperation.DISCARD && !info.isDelState())
                .toList();
        if (infoList.size() > 1) {
            log.error("出现错误 未弃牌人数大于1");
            normalSettlement(controller);
            return;
        }
        long playerId = infoList.getFirst().getPlayerId();
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
        Long allBet = baseBetInfo.getOrDefault(playerId, 0L);
        long beforeRatio = total - allBet;
        //添加记录
        TexasSaveHistory texasHistory = gameDataVo.getTexasHistory();
        for (TexasHistoryPlayerInfo info : texasHistory.getTotalPlayerBetInfo()) {
            if (info.playerId == playerId) {
                info.betValue = beforeRatio;
                continue;
            }
            info.betValue = -info.betValue;
        }
        long get = beforeRatio * (10000 - gameDataVo.getRoomCfg().getEffectiveRatio()) / 10000;
        controller.changePlayerGold(gamePlayer, allBet + get);
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
        // 添加账单记录
        settlementDataMap.put(
                playerId, settlementDataMap.getOrDefault(playerId, new SettlementData())
                        .increaseBySettlementData(new SettlementData(
                                get, allBet, beforeRatio, allBet, beforeRatio - get)));

        addCreateRecord(controller, settlementDataMap);
        pokerPlayerSettlementInfo.currentGold = gameDataVo.getTempGold().getOrDefault(playerId, 0L);
        pokerPlayerSettlementInfo.getGold = allBet + get;
        pokerPlayerSettlementInfo.win = pokerPlayerSettlementInfo.getGold > 0;
        TexasSettlementPlayerInfo settlementPlayerInfo = new TexasSettlementPlayerInfo();
        settlementPlayerInfo.pokerPlayerSettlementInfo = pokerPlayerSettlementInfo;
        settlementInfoArrayList.add(settlementPlayerInfo);
        notifyTexasSettlementInfo.playerSettlementInfos = settlementInfoArrayList;
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasSettlementInfo));
        gameDataVo.setNotifyTexasSettlementInfo(notifyTexasSettlementInfo);
    }

    public void addLog(TexasGameController controller, TexasSaveHistory texasSaveHistory) {
        TexasHistory texasHistory = controller.buildTexasHistory(0, texasSaveHistory);
        TexasGameDataVo gameDataVo = controller.getGameDataVo();
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        ActivityManager activityManager = controller.getRoomController().getRoomManager().getActivityManager();
        //构建玩家信息
        for (TexasHistoryPlayerInfo info : texasHistory.totalPlayerBetInfo) {
            Long betValue = baseBetInfo.getOrDefault(info.playerId, 0L);
            SimplePlayerInfo simplePlayerInfo = new SimplePlayerInfo(info.playerId, info.playerName);
            gameDataTracker.addPlayerLogData(simplePlayerInfo, DataTrackNameConstant.TOTAL_BET, betValue);
            gameDataTracker.addPlayerLogData(
                    simplePlayerInfo, DataTrackNameConstant.TOTAL_WIN, betValue + info.betValue);
            gameDataTracker.addPlayerLogData(simplePlayerInfo, DataTrackNameConstant.INCOME, info.betValue);
            gameDataTracker.addPlayerLogData(simplePlayerInfo, DataTrackNameConstant.EFFECTIVE_BET, betValue);
            //增加个人
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(info.playerId);
            if (!(gamePlayer instanceof GameRobotPlayer)) {
                Thread.ofVirtual().start(() -> {
                    try {
                        activityManager.addPlayerActivityProgress(gamePlayer,
                                ActivityTargetType.getTagetKey(ActivityTargetType.EFFECTIVE_BET), betValue,
                                controller.getGameTransactionItemId());
                        activityManager.addActivityProgress(gamePlayer,
                                ActivityTargetType.getTagetKey(ActivityTargetType.EFFECTIVE_BET), betValue,
                                controller.getGameTransactionItemId());
                        // 触发事件
                        gameController.getGameEventManager().triggerEvent(
                                new PlayerEffectiveFlowingEvent(gamePlayer, gameDataVo.getRoomCfg().getId(), betValue, 0));
                        //触发任务
                        gameController.getTaskManager().trigger(gamePlayer.getId(), TaskConstant.ConditionType.PLAYER_BET_ALL, () -> {
                            TaskConditionParam12001 param = new TaskConditionParam12001();
                            param.setGameId(gameDataVo.getRoomCfg().getGameID());
                            param.setAddValue(betValue);
                            return param;
                        });
                        //触发任务
                        if (info.betValue > 0) {
                            gameController.triggerTask(gamePlayer.getId(), gameController.getRoom().getGameType(), info.betValue, gameController.getGameTransactionItemId());
                        }
                    } catch (Exception e) {
                        log.error("德州日志添加进度失败 info:{}", gamePlayer.getId(), e);
                    }
                });
            }
        }
        //增加总体
        gameDataTracker.addGameLogData("TexasInfo", texasHistory);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }


    @Override
    public void phaseFinishDoAction() {
        //设置为等待阶段
        //暂定50条
        if (gameDataVo.getTexasHistoryList().size() >= 50) {
            gameDataVo.getTexasHistoryList().removeLast();
        }
        gameDataVo.getTexasHistoryList().add(gameDataVo.getTexasHistory());
        //结算后更新玩家信息
        updatePlayerData();
    }

    /**
     * 结算后更新玩家信息
     */
    public void updatePlayerData() {
        if (gameController instanceof TexasGameController controller) {
            List<PokerPlayerInfo> infos = new ArrayList<>();
            for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
                seatInfo.setJoinGame(false);
                seatInfo.setReady(false);
                //更新操作时间
                controller.updatePlayerLatestOperateTime(seatInfo.getPlayerId());
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
                if (Objects.isNull(gamePlayer)) {
                    continue;
                }
                controller.addTempGoldOrOutTable(seatInfo, gamePlayer);
                infos.add(PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, controller));
            }
            NotifyTexasSettlementPlayerChange notifySettlementPlayerChange = new NotifyTexasSettlementPlayerChange();
            notifySettlementPlayerChange.pokerPlayerInfos = infos;
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifySettlementPlayerChange));
        }
    }

}
