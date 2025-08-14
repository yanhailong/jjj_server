package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.data.MaxPointGetInfo;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackSettlementInfo;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.common.gamephase.BaseSettlementPhase;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;
import com.jjg.game.poker.game.sample.bean.BlackjackCfg;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;

/**
 * @author lm
 * @date 2025/7/29 09:31
 */
public class BlackJackSettlementPhase extends BaseSettlementPhase<BlackJackGameDataVo> {

    public BlackJackSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (gameController instanceof BlackJackGameController controller) {
            //庄家发牌
            dealSettlement(controller);
        }
    }

    /**
     * 处理庄家的牌
     */
    public void dealSettlement(BlackJackGameController controller) {
        //获取总点数
        List<Integer> dealerCards = gameDataVo.getDealerCards();
        //配置信息
        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
        Room_ChessCfg chessCfg = gameDataVo.getRoomCfg();
        List<Integer> resultCard = new ArrayList<>(dealerCards);
        int maxCardNum = BlackJackConstant.Common.MAX_GET_CARD - chessCfg.getHandPoker();
        for (int i = 0; i < maxCardNum; i++) {
            resultCard.add(controller.getCard(gameDataVo));
        }
        //获取庄家最大点数
        Pair<MaxPointGetInfo, Boolean> maxPointInfoPair = getMaxPointInfo(resultCard);
        MaxPointGetInfo maxPointInfo = maxPointInfoPair.getFirst();
        boolean cardNumWin = maxPointInfo.getIndex() + 1 == maxCardNum;
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
        List<Integer> sendCards = cardNumWin ? resultCard : resultCard.subList(0, boom ? maxPointInfo.getIndex() + 2 : maxPointInfo.getIndex() + 1);
        settlementPlayerInfo.cardIds = BlackJackDataHelper.getClientId(sendCards, BlackJackDataHelper.getPoolId(gameDataVo));
        settlementPlayerInfo.totalPoint = BlackJackDataHelper.getTotalPoint(sendCards);
        settlementPlayerInfo.endTime = gameDataVo.getPhaseEndTime() + (long) sendCards.size() * PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS);
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            long playerId = info.getPlayerId();
            for (int i = 0; i < info.getCards().size(); i++) {
                List<Integer> card = info.getCards().get(i);
                int point = BlackJackDataHelper.getTotalPoint(card);
                if (point > BlackJackConstant.Common.PERFECT_POINT) {
                    continue;
                }
                Map<Integer, Long> betInfo = allBetInfo.get(playerId);
                Long betValue = betInfo.getOrDefault(i, 0L);
                if (betValue == 0) {
                    continue;
                }
                //初始为21点 直接发奖
                if (point == BlackJackConstant.Common.PERFECT_POINT && info.getCards().size() == 1 && info.getCurrentCards().size() == chessCfg.getHandPoker()) {
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getBlackjack()), Long::sum);
                    continue;
                }
                //连续6张直接发奖
                if (card.size() == maxCardNum) {
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getFiveLittleDragons()), Long::sum);
                    continue;
                }
                //庄家6张直接获胜
                if (cardNumWin) {
                    continue;
                }
                //判断庄家和玩家点数
                if (boom || maxPointInfo.getMaxPoint() < point) {
                    int param = point == BlackJackConstant.Common.PERFECT_POINT ? blackjackCfg.getTwentyOne() : blackjackCfg.getOther();
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, param), Long::sum);
                }
                if (maxPointInfo.getMaxPoint() == point) {
                    playerGet.merge(playerId, BlackJackDataHelper.getGetWinValue(betValue, blackjackCfg.getDraw()), Long::sum);
                }
            }
            PokerPlayerSettlementInfo blackJackSettlementInfo = new PokerPlayerSettlementInfo();
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
            blackJackSettlementInfo.win = get > 0;
            blackJackSettlementInfo.currentGold = Objects.isNull(gamePlayer) ? 0 : gamePlayer.getGold();
            settlementPlayerInfo.settlementInfos.add(blackJackSettlementInfo);
        }
        gameDataVo.setSettlementInfo(settlementPlayerInfo);
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(settlementPlayerInfo));
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
        return Pair.newPair(totalPointList.get(0), hasA);
    }


}
