package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.common.proto.Pair;
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
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BlackjackCfg;
import com.jjg.game.sampledata.bean.PokerPoolCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

import static com.jjg.game.common.constant.CoreConst.Common.SAMPLE_ROOT_PATH;

/**
 * @AUTHOR LM
 * @DATE 2025/7/29 09:31
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
        if (gameController instanceof BlackJackGameController controller) {
            int settlementType = gameDataVo.getSettlementType();
            switch (settlementType) {
                case 1 -> dealSpecialSettlement(controller);
                case 2 -> dealNormalSpecialSettlement(controller);
                default -> dealSettlement(controller);
            }
        }
    }

    /**
     * 处理正常特殊结算
     */
    public void dealNormalSpecialSettlement(BlackJackGameController controller) {
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        NotifyBlackJackSpecialSettlement notifyBlackJackSpecialSettlement = new NotifyBlackJackSpecialSettlement();
        notifyBlackJackSpecialSettlement.type = seatInfo.getOperationType();
        notifyBlackJackSpecialSettlement.playerId = seatInfo.getPlayerId();
        notifyBlackJackSpecialSettlement.sendCardId = BlackJackDataHelper.getClientCardId(gameDataVo,seatInfo.getCurrentCards().getLast());
        notifyBlackJackSpecialSettlement.putCardTotal = BlackJackDataHelper.getTotalPoint(seatInfo.getCurrentCards());
        if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.DOUBLE_BET) {
            notifyBlackJackSpecialSettlement.betValue = gameDataVo.getBaseBetInfo().getOrDefault(seatInfo.getPlayerId(), 0L);
        }
        notifyBlackJackSpecialSettlement.cardIdList = BlackJackBuilder.getCardInfos(seatInfo, controller);
        notifyBlackJackSpecialSettlement.currentCardIds = seatInfo.getCards().size() > 1 ? seatInfo.getCardIndex() + 1 : seatInfo.getCardIndex();
        notifyBlackJackSpecialSettlement.settlementInfo = normalSettlement(controller);
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackSpecialSettlement));
    }

    /**
     * 处理特殊结算
     */
    public void dealSpecialSettlement(BlackJackGameController controller) {
        PlayerSeatInfo seatInfo = gameDataVo.getCurrentPlayerSeatInfo();
        NotifyBlackJackSpecialSettlement notifyBlackJackSpecialSettlement = new NotifyBlackJackSpecialSettlement();
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
            notifyBlackJackSpecialSettlement.putCardTotal = BlackJackDataHelper.getTotalPoint(seatInfo.getCards().getFirst());
        }
        if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.STOP) {
            notifyBlackJackSpecialSettlement.autoCard = BlackJackDataHelper.getClientCardId(gameDataVo, seatInfo.getCurrentCards().getLast());
        }
        if (seatInfo.getOperationType() == PokerConstant.PlayerOperation.DOUBLE_BET) {
            notifyBlackJackSpecialSettlement.sendCardId = BlackJackDataHelper.getClientCardId(gameDataVo,seatInfo.getCards().getFirst().getLast());
            notifyBlackJackSpecialSettlement.autoCard = BlackJackDataHelper.getClientCardId(gameDataVo,seatInfo.getCurrentCards().getLast());
            notifyBlackJackSpecialSettlement.putCardTotal = BlackJackDataHelper.getTotalPoint(seatInfo.getCards().getFirst());
            notifyBlackJackSpecialSettlement.betValue = gameDataVo.getBaseBetInfo().getOrDefault(seatInfo.getPlayerId(), 0L);
        }
        notifyBlackJackSpecialSettlement.cardIdList = BlackJackBuilder.getCardInfos(seatInfo, controller);
        notifyBlackJackSpecialSettlement.currentCardIds = seatInfo.getCardIndex();
        notifyBlackJackSpecialSettlement.settlementInfo = normalSettlement(controller);
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackSpecialSettlement));
    }

    /**
     * 正常结算
     */
    public NotifyBlackJackSettlementInfo normalSettlement(BlackJackGameController controller) {
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
        Pair<MaxPointGetInfo, Boolean> maxPointInfoPair = getMaxPointInfo(resultCard);
        MaxPointGetInfo maxPointInfo = maxPointInfoPair.getFirst();
        boolean cardNumWin = maxPointInfo.getIndex() + 1 == BlackJackConstant.Common.MAX_GET_CARD;
        boolean boom = false;
        if (maxPointInfoPair.getSecond()) {
            boom = !cardNumWin && (maxPointInfo.getMaxPoint() < 17 || maxPointInfo.isSoftHand() && maxPointInfo.getMaxPoint() == 17);
        }
        //玩家id->获得的金币
        Map<Long, Long> playerGet = new HashMap<>();
        //押注信息
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        Map<Long, Map<Integer, Long>> allBetInfo = gameDataVo.getAllBetInfo();

        Set<Long> aceBuyPlayerIds = gameDataVo.getAceBuyPlayerIds();
        if (gameDataVo.isCanBuyACE() && !aceBuyPlayerIds.isEmpty() && maxPointInfo.getMaxPoint() == BlackJackConstant.Common.PERFECT_POINT) {
            //购买ACE发奖
            for (Long playerId : aceBuyPlayerIds) {
                int insurance = blackjackCfg.getInsurance();
                Long betValue = baseBetInfo.getOrDefault(playerId, 0L);
                playerGet.put(playerId, BlackJackDataHelper.getGetWinValue(betValue, insurance));
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
        settlementPlayerInfo.endTime = gameDataVo.getPhaseEndTime() + (showDealer ? (long) sendCards.size() * PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS) : 0);
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            long playerId = info.getPlayerId();
            BlackJackSettlementInfo settlementInfo = new BlackJackSettlementInfo();
            int size = info.getCards().size();
            //获胜牌组索引
            settlementInfo.cardGroupState = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                List<Integer> card = info.getCards().get(i);
                int point = BlackJackDataHelper.getTotalPoint(card);
                if (point > BlackJackConstant.Common.PERFECT_POINT) {
                    settlementInfo.cardGroupState.add(0);
                    continue;
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
                //初始为21点 直接发奖
                if (point == BlackJackConstant.Common.PERFECT_POINT && info.getCurrentCards().size() == chessCfg.getHandPoker()) {
                    if (tianHu) {
                        settlementInfo.cardGroupState.add(2);
                        continue;
                    }
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getBlackjack()), Long::sum);
                    settlementInfo.cardGroupState.add(1);
                    continue;
                }
                if (tianHu) {
                    settlementInfo.cardGroupState.add(0);
                    continue;
                }
                //连续6张直接发奖
                if (card.size() == maxCardNum) {
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getFiveLittleDragons()), Long::sum);
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
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, param), Long::sum);
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
            long get = playerGet.getOrDefault(playerId, 0L) - controller.getPlayerTotalBet(playerId);
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
            if (Objects.isNull(gamePlayer)) {
                log.error("21点发奖找不到GamePlayer playerId:{} get:{} id:{}", playerId, get, gameDataVo.getId());
            }
            if (get > 0 && Objects.nonNull(gamePlayer)) {
                gamePlayer.setGold(gamePlayer.getGold() + get);
            }
            blackJackSettlementInfo.getGold = get;
            blackJackSettlementInfo.win = get >= 0;
            blackJackSettlementInfo.currentGold = Objects.isNull(gamePlayer) ? 0 : gamePlayer.getGold();
            settlementPlayerInfo.settlementInfos.add(settlementInfo);
        }
        gameDataVo.setSettlementInfo(settlementPlayerInfo);
        return settlementPlayerInfo;
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
    public static Pair<MaxPointGetInfo, Boolean> getMaxPointInfo(List<Integer> maxGetCard) {
        List<MaxPointGetInfo> totalPointList = new ArrayList<>();
        MaxPointGetInfo base = new MaxPointGetInfo(0, 0, false);
        totalPointList.add(base);
        boolean isMax = false;
        boolean hasA = false;
        for (int i = 0; i < maxGetCard.size(); i++) {
            int card = maxGetCard.get(i);
            int point = BlackJackDataHelper.getCfgPoint(card);
            int endNum = 0;
            for (MaxPointGetInfo value : totalPointList) {
                if (value.getMaxPoint() + point <= BlackJackConstant.Common.PERFECT_POINT) {
                    value.setMaxPoint(value.getMaxPoint() + point);
                    value.setIndex(i);
                } else {
                    endNum++;
                }
            }
            if (endNum >= totalPointList.size()) {
                break;
            }
            //总点数,索引
            if (!isMax && point == 1) {
                hasA = true;
                isMax = base.getMaxPoint() + 10 > BlackJackConstant.Common.PERFECT_POINT;
                if (!isMax) {
                    totalPointList.add(new MaxPointGetInfo(base.getMaxPoint() + 10, i, true));
                }
            }

        }
        totalPointList.sort((o1, o2) -> {
            if (o1.getIndex() == maxGetCard.size()) {
                return 1;
            }
            if (o2.getIndex() == maxGetCard.size()) {
                return -1;
            }
            int result = o2.getMaxPoint() - o1.getMaxPoint();
            if (result == 0) {
                result = o2.getIndex() - o1.getIndex();
            }
            return result;
        });
        return Pair.newPair(totalPointList.getFirst(), hasA);
    }

    public static void main(String[] args) throws Exception {
        GameDataManager.loadAllData(SAMPLE_ROOT_PATH);
        BlackJackDataHelper.initData();
        Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(100001);
        ArrayList<Integer> card = new ArrayList<>(cardListMap.keySet());
        int maxCardNum = 2 + BlackJackConstant.Common.MAX_GET_CARD;
        Collections.shuffle(card);
        List<Integer> resultCard = new ArrayList<>();
        resultCard.add(41);
        resultCard.add(25);
        resultCard.add(1);
        resultCard.add(2);
        resultCard.add(29);
        resultCard.add(45);
//            for (int i = 0; i < (BlackJackConstant.Common.MAX_GET_CARD + 2); i++) {
//                resultCard.add(card.remove(0));
//            }
        //获取庄家最大点数
        Pair<MaxPointGetInfo, Boolean> maxPointInfoPair = getMaxPointInfo(resultCard);
        MaxPointGetInfo maxPointInfo = maxPointInfoPair.getFirst();
        boolean cardNumWin = maxPointInfo.getIndex() + 1 == maxCardNum;
        boolean boom = false;
        if (maxPointInfoPair.getSecond()) {
            boom = !cardNumWin && (maxPointInfo.getMaxPoint() < 17 || maxPointInfo.isSoftHand() && maxPointInfo.getMaxPoint() == 17);
        }
        List<Integer> sendCards = cardNumWin ? resultCard : resultCard.subList(0, Math.min(resultCard.size(), boom ? maxPointInfo.getIndex() + 2 : maxPointInfo.getIndex() + 1));
        BlackJackDataHelper.getTotalPoint(sendCards);
        System.out.println(sendCards);
        int totalPoint = BlackJackDataHelper.getTotalPoint(sendCards);
        System.out.println("总点数" + totalPoint);
        for (Integer sendCard : sendCards) {
            PokerPoolCfg pokerPoolCfg = GameDataManager.getPokerPoolCfg(sendCard);
            System.out.println("点数：" + pokerPoolCfg.getPointsNum());
        }

    }

}
