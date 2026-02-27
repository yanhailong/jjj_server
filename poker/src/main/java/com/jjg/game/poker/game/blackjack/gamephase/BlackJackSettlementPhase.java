package com.jjg.game.poker.game.blackjack.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.data.BlackJackBuilder;
import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.data.MaxPointGetInfo;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackSettlementInfo;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackSettlementInfo;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackSpecialSettlement;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.BlackjackCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author LM
 * @date 2025/7/29 09:31
 */
public class BlackJackSettlementPhase extends BaseSettlementPhase<BlackJackGameDataVo> {

    public BlackJackSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController) {
        super(gameController);
    }


    @Override
    public int getPhaseRunTime() {
        return super.getPhaseRunTime() + gameDataVo.getSettlementDelayTime();
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        try {
            if (gameController instanceof BlackJackGameController controller) {
                int settlementType = gameDataVo.getSettlementType();
                switch (settlementType) {
                    case 1 -> dealSpecialSettlement(controller);
                    case 2 -> dealNormalSpecialSettlement(controller);
                    default -> dealSettlement(controller);
                }
            }
        } catch (Exception e) {
            log.error("21点结算异常", e);
        }
    }

    /**
     * 处理正常特殊结算
     */
    public void dealNormalSpecialSettlement(BlackJackGameController controller) {
        NotifyBlackJackSpecialSettlement notifyBlackJackSpecialSettlement = new NotifyBlackJackSpecialSettlement();
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (seatInfo != null) {
            notifyBlackJackSpecialSettlement.type = seatInfo.getOperationType();
            notifyBlackJackSpecialSettlement.playerId = seatInfo.getPlayerId();
            notifyBlackJackSpecialSettlement.sendCardId = BlackJackDataHelper.getClientCardId(gameDataVo, seatInfo.getCurrentCards().getLast());
            notifyBlackJackSpecialSettlement.putCardTotal = BlackJackDataHelper.getShowTotalPoint(seatInfo.getCurrentCards());
            if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.DOUBLE_BET) {
                notifyBlackJackSpecialSettlement.betValue = gameDataVo.getBaseBetInfo().getOrDefault(seatInfo.getPlayerId(), 0L);
            }
            notifyBlackJackSpecialSettlement.betValueList = gameDataVo.getPlayerBetValueList().getOrDefault(seatInfo.getPlayerId(), new ArrayList<>());
            notifyBlackJackSpecialSettlement.cardIdList = BlackJackBuilder.getCardInfos(seatInfo, controller);
            notifyBlackJackSpecialSettlement.currentCardIds = seatInfo.getCards().size() > 1 ? seatInfo.getCardIndex() + 1 : seatInfo.getCardIndex();
        }
        notifyBlackJackSpecialSettlement.settlementInfo = normalSettlement(controller);
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackSpecialSettlement));
    }

    /**
     * 处理特殊结算
     */
    public void dealSpecialSettlement(BlackJackGameController controller) {
        NotifyBlackJackSpecialSettlement notifyBlackJackSpecialSettlement = new NotifyBlackJackSpecialSettlement();
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (seatInfo != null) {
            notifyBlackJackSpecialSettlement.type = seatInfo.getOperationType();
            if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.NONE) {
                notifyBlackJackSpecialSettlement.cardIdList = BlackJackBuilder.getBlackJackCardInfoList(gameDataVo);
                notifyBlackJackSpecialSettlement.cardId = BlackJackDataHelper.getClientCardId(gameDataVo, gameDataVo.getDealerCards().getFirst());
                notifyBlackJackSpecialSettlement.settlementInfo = normalSettlement(controller);
                broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackSpecialSettlement));
                return;
            }
            notifyBlackJackSpecialSettlement.playerId = seatInfo.getPlayerId();
            if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.GET_CARD) {
                //第一组的最后一张牌
                notifyBlackJackSpecialSettlement.sendCardId = BlackJackDataHelper.getClientCardId(gameDataVo, seatInfo.getCards().getFirst().getLast());
                notifyBlackJackSpecialSettlement.autoCard = BlackJackDataHelper.getClientCardId(gameDataVo, seatInfo.getCurrentCards().getLast());
                notifyBlackJackSpecialSettlement.putCardTotal = BlackJackDataHelper.getShowTotalPoint(seatInfo.getCards().getFirst());
            }
            if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.STOP) {
                notifyBlackJackSpecialSettlement.autoCard = BlackJackDataHelper.getClientCardId(gameDataVo, seatInfo.getCurrentCards().getLast());
            }
            if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.DOUBLE_BET) {
                notifyBlackJackSpecialSettlement.sendCardId = BlackJackDataHelper.getClientCardId(gameDataVo, seatInfo.getCards().getFirst().getLast());
                notifyBlackJackSpecialSettlement.autoCard = BlackJackDataHelper.getClientCardId(gameDataVo, seatInfo.getCurrentCards().getLast());
                notifyBlackJackSpecialSettlement.putCardTotal = BlackJackDataHelper.getShowTotalPoint(seatInfo.getCards().getFirst());
                notifyBlackJackSpecialSettlement.betValue = gameDataVo.getBaseBetInfo().getOrDefault(seatInfo.getPlayerId(), 0L);
            }
            notifyBlackJackSpecialSettlement.betValueList = gameDataVo.getPlayerBetValueList().getOrDefault(seatInfo.getPlayerId(), new ArrayList<>());
            notifyBlackJackSpecialSettlement.cardIdList = BlackJackBuilder.getCardInfos(seatInfo, controller);
            notifyBlackJackSpecialSettlement.currentCardIds = seatInfo.getCardIndex();
        }
        notifyBlackJackSpecialSettlement.settlementInfo = normalSettlement(controller);
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackSpecialSettlement));
    }

    /**
     * 正常结算
     */
    public NotifyBlackJackSettlementInfo normalSettlement(BlackJackGameController controller) {
        Pair<Long, Long> poolPair = canTriggerRecycling();
        if (poolPair != null) {
            log.error("21点结算 当前池:{} 标准池:{}", poolPair.getFirst(), poolPair.getSecond());
        }
        //获取总点数
        List<Integer> dealerCards = gameDataVo.getDealerCards();
        boolean showDealer = gameDataVo.isShowDealer();
        //配置信息
        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
        Room_ChessCfg chessCfg = gameDataVo.getRoomCfg();
        int totalPoint = BlackJackDataHelper.getTotalPoint(dealerCards);
        boolean tianHu = totalPoint == BlackJackConstant.Common.PERFECT_POINT;
        List<Integer> resultCard = new ArrayList<>(dealerCards);
        int maxCardNum = BlackJackConstant.Common.MAX_GET_CARD - chessCfg.getHandPoker();
        if (!tianHu && showDealer) {
            for (int i = 0; i < maxCardNum; i++) {
                resultCard.add(controller.getCard(gameDataVo));
            }
        }
        //获取庄家最大点数
        MaxPointGetInfo maxPointInfo = getMaxPointInfo(resultCard);
        boolean cardNumWin = maxPointInfo.getIndex() + 1 == BlackJackConstant.Common.MAX_GET_CARD;
        boolean boom = !cardNumWin && (maxPointInfo.getMaxPoint() < BlackJackConstant.Common.GET_CARD_POINT);
        //玩家id->获得的金币
        Map<Long, Long> playerGet = new HashMap<>();
        //押注信息
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        Map<Long, Map<Integer, Long>> allBetInfo = gameDataVo.getAllBetInfo();
        //计算总税收
        Map<Long, Long> totalTax = new HashMap<>(0);
        Set<Long> aceBuyPlayerIds = gameDataVo.getAceBuyPlayerIds();
        long poolWinValue = 0;
        long poolLoseValue = 0;
        if (gameDataVo.isCanBuyACE() && !aceBuyPlayerIds.isEmpty()) {
            for (Long playerId : aceBuyPlayerIds) {
                int insurance = blackjackCfg.getInsurance();
                Long betValue = baseBetInfo.getOrDefault(playerId, 0L);
                if (tianHu) {
                    //购买ACE发奖
                    playerGet.put(playerId, getGetRadioAfterValue(playerId, totalTax, betValue, BlackJackDataHelper.getGetWinValue(betValue, insurance)));
                    poolLoseValue -= betValue;
                } else {
                    poolWinValue += betValue;
                }
            }

        }
        //结算信息
        NotifyBlackJackSettlementInfo settlementPlayerInfo = new NotifyBlackJackSettlementInfo();
        settlementPlayerInfo.settlementInfos = new ArrayList<>();
        List<Integer> sendCards = cardNumWin ? resultCard : resultCard.subList(0, Math.min(resultCard.size(), boom ? maxPointInfo.getIndex() + 2 : maxPointInfo.getIndex() + 1));
        settlementPlayerInfo.cardIds = BlackJackDataHelper.getClientId(gameDataVo, sendCards);
        int dealerTotalPoint = BlackJackDataHelper.getTotalPoint(sendCards);
        settlementPlayerInfo.totalPoint = dealerTotalPoint;
        settlementPlayerInfo.showDealer = showDealer;
        PlayerSeatInfo playerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        if (playerSeatInfo != null) {
            settlementPlayerInfo.playerId = playerSeatInfo.getPlayerId();
            settlementPlayerInfo.type = playerSeatInfo.getOperationType();
        }
        settlementPlayerInfo.endTime = gameDataVo.getPhaseEndTime() + (showDealer ? (long) sendCards.size() * PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS) : 0);

        Map<Long, List<List<Integer>>> playerCards = new HashMap<>();
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) {
                continue;
            }
            long playerId = info.getPlayerId();
            BlackJackSettlementInfo settlementInfo = new BlackJackSettlementInfo();
            int size = info.getCards().size();
            boolean robot = RobotUtil.isRobot(info.getPlayerId());
            //获胜牌组索引
            settlementInfo.cardGroupState = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                List<Integer> card = info.getCards().get(i);
                if (!robot) {
                    playerCards.computeIfAbsent(playerId, key -> new ArrayList<>()).add(BlackJackDataHelper.getClientId(gameDataVo, card));
                }
                Map<Integer, Long> betInfo = allBetInfo.get(playerId);
                if (Objects.isNull(betInfo)) {
                    continue;
                }
                Long betValue = betInfo.getOrDefault(i, 0L);
                if (betValue == 0) {
                    settlementInfo.cardGroupState.add(2);
                    continue;
                }
                int point = BlackJackDataHelper.getTotalPoint(card);
                if (point > BlackJackConstant.Common.PERFECT_POINT) {
                    settlementInfo.cardGroupState.add(0);
                    continue;
                }
                //初始为21点 直接发奖
                if (point == BlackJackConstant.Common.PERFECT_POINT && card.size() == chessCfg.getHandPoker()) {
                    if (tianHu) {
                        settlementInfo.cardGroupState.add(2);
                        continue;
                    }
                    playerGet.merge(playerId, getGetRadioAfterValue(playerId, totalTax, betValue, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getBlackjack())), Long::sum);
                    settlementInfo.cardGroupState.add(1);
                    continue;
                }
                if (tianHu) {
                    settlementInfo.cardGroupState.add(0);
                    continue;
                }
                //连续6张直接发奖
                if (card.size() == BlackJackConstant.Common.MAX_GET_CARD) {
                    playerGet.merge(playerId, getGetRadioAfterValue(playerId, totalTax, betValue, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getFiveLittleDragons())), Long::sum);
                    settlementInfo.cardGroupState.add(1);
                    continue;
                }
                //庄家6张直接获胜
                if (cardNumWin) {
                    settlementInfo.cardGroupState.add(0);
                    continue;
                }
                //判断庄家和玩家点数
                if (boom || dealerTotalPoint < point) {
                    int param = point == BlackJackConstant.Common.PERFECT_POINT ? blackjackCfg.getTwentyOne() : blackjackCfg.getOther();
                    playerGet.merge(playerId, getGetRadioAfterValue(playerId, totalTax, betValue, BlackJackDataHelper.getGetWinValue(betValue, param)), Long::sum);
                    settlementInfo.cardGroupState.add(1);
                    continue;
                }
                if (dealerTotalPoint == point) {
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getDraw()), Long::sum);
                    settlementInfo.cardGroupState.add(2);
                    continue;
                }
                settlementInfo.cardGroupState.add(0);
            }
            PokerPlayerSettlementInfo blackJackSettlementInfo = new PokerPlayerSettlementInfo();
            settlementInfo.settlementInfo = blackJackSettlementInfo;
            blackJackSettlementInfo.playerId = playerId;
            Long totalGet = playerGet.getOrDefault(playerId, 0L);
            long totalBet = controller.getPlayerTotalBet(playerId);
            long get = totalGet - totalBet + totalTax.getOrDefault(playerId, 0L);
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
            if (gamePlayer == null) {
                log.error("21点结算时 gamePlayer=null playerId:{}", playerId);
                continue;
            }
            if (get >= 0) {
                if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                    robotPlayer.setLastWin(1);
                } else {
                    poolLoseValue += get;
                }
            } else {
                if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                    robotPlayer.setLastWin(2);
                } else {
                    gameController.dealLose(gamePlayer, get);
                    poolWinValue += Math.abs(get);
                }
            }
            if (totalGet > 0) {
                gameController.addItem(playerId, totalGet, AddType.GAME_SETTLEMENT);
                playerGet.put(playerId, totalGet);
            }
            blackJackSettlementInfo.getGold = totalGet;
            blackJackSettlementInfo.win = get >= 0;
            blackJackSettlementInfo.currentGold = controller.getTransactionItemNum(playerId);
            settlementPlayerInfo.settlementInfos.add(settlementInfo);
        }
        totalTax.keySet().removeIf(RobotUtil::isRobot);
        gameDataTracker.addGameLogData("tax", totalTax);
        gameDataTracker.addGameLogData("dealerCards", settlementPlayerInfo.cardIds);
        gameDataTracker.addGameLogData("playerCards", playerCards);
        log.info("21点结算信息: {}", JSON.toJSONString(settlementPlayerInfo));
        addLog(controller, playerGet);
        gameDataVo.setSettlementInfo(settlementPlayerInfo);
        dealRoomPool(poolWinValue, poolLoseValue);
        return settlementPlayerInfo;
    }

    /**
     * 获取抽水后的金额
     *
     * @param totalTax
     * @param betValue
     * @param get      获得金额
     * @return 抽水后金额
     */
    private long getGetRadioAfterValue(long playerId, Map<Long, Long> totalTax, long betValue, long get) {
        long beforeCalculation = get - betValue;
        long realGetValue = BigDecimal.valueOf(beforeCalculation)
                .multiply(BigDecimal.valueOf(10000 - gameDataVo.getRoomCfg().getWinRatio()))
                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();
        totalTax.merge(playerId, beforeCalculation - realGetValue, Long::sum);
        return realGetValue + betValue;
    }


    public void addLog(BlackJackGameController controller, Map<Long, Long> playerGet) {
        //构建玩家信息
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            long playerId = playerSeatInfo.getPlayerId();
            if (RobotUtil.isRobot(playerId)) {
                continue;
            }
            long totalBet = controller.getPlayerTotalBet(playerId);
            Long win = playerGet.getOrDefault(playerId, 0L);
            Player gamePlayer = controller.getGamePlayer(playerId);
            if (gamePlayer == null) {
                //从内存加载
                CorePlayerService playerService = controller.getRoomController().getRoomManager().getPlayerService();
                gamePlayer = playerService.get(playerId);
            }
            if (gamePlayer == null) {
                continue;
            }
            Set<Long> aceBuyPlayerIds = gameDataVo.getAceBuyPlayerIds();
            if (aceBuyPlayerIds.contains(playerId)) {
                totalBet += gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
            }
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_BET, totalBet);
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.TOTAL_WIN, win);
            long income = win - totalBet;
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.INCOME, income);
            gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.EFFECTIVE_BET, totalBet);
            //增加个人
            if (income > 0) {
                //触发任务
                gameController.triggerSettlementAction(gamePlayer.getId(), gameController.getRoom().getGameType(), 0,
                        income, gameController.getGameTransactionItemId());
            } else {
                gameController.dealLose(gamePlayer, income);
            }
        }
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    /**
     * 处理庄家的牌
     */
    public void dealSettlement(BlackJackGameController controller) {
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(normalSettlement(controller)));
    }


    /**
     * 返回拿的牌id
     * 庄家只有在“硬 17+”时才会停牌；其余情况一律继续叫牌。
     */
    public static MaxPointGetInfo getMaxPointInfo(List<Integer> maxGetCard) {
        List<MaxPointGetInfo> totalPointList = new ArrayList<>();
        MaxPointGetInfo base = new MaxPointGetInfo(0, 0, false, false);
        totalPointList.add(base);
        boolean isMax = false;
        for (int i = 0; i < maxGetCard.size(); i++) {
            int card = maxGetCard.get(i);
            int point = BlackJackDataHelper.getCfgPoint(card);
            int endNum = 0;
            for (MaxPointGetInfo value : totalPointList) {
                if (value.isEnd()) {
                    endNum++;
                    continue;
                }
                if (value.getMaxPoint() + point <= BlackJackConstant.Common.PERFECT_POINT) {
                    value.setMaxPoint(value.getMaxPoint() + point);
                    value.setIndex(i);
                    continue;
                }
                endNum++;
                value.setEnd(true);
            }
            if (endNum >= totalPointList.size()) {
                break;
            }
            //总点数,索引
            if (!isMax && point == 1) {
                isMax = base.getMaxPoint() + 10 > BlackJackConstant.Common.PERFECT_POINT;
                if (!isMax) {
                    totalPointList.add(new MaxPointGetInfo(base.getMaxPoint() + 10, i, true, false));
                }
            }
        }
        totalPointList.sort((o1, o2) -> {
            if (o1.getIndex() == maxGetCard.size() - 1) {
                return 1;
            }
            if (o2.getIndex() == maxGetCard.size() - 1) {
                return -1;
            }
            int result = o2.getMaxPoint() - o1.getMaxPoint();
            if (result == 0) {
                result = o2.getIndex() - o1.getIndex();
            }
            return result;
        });
        //找出非硬手17点最大的
        for (MaxPointGetInfo info : totalPointList) {
            if (info.getIndex() == maxGetCard.size() - 1) {
                return info;
            }
            if (info.isSoftHand() && info.getMaxPoint() <= BlackJackConstant.Common.GET_CARD_POINT) {
                continue;
            }
            return info;
        }
        return totalPointList.getFirst();
    }

}
