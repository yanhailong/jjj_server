package com.jjg.game.table.redblackwar.gamephase;

import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.datatrack.SaveLogUtil;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.redblackwar.constant.HandType;
import com.jjg.game.table.redblackwar.constant.RedBlackWarConstant;
import com.jjg.game.table.redblackwar.manager.RedBlackWarSampleManager;
import com.jjg.game.table.redblackwar.message.bean.RedBlackWarHistory;
import com.jjg.game.table.redblackwar.message.resp.NotifyRedBlackWarSettleInfo;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import com.jjg.game.table.redblackwar.util.CardComparatorUtil;
import com.jjg.game.table.redblackwar.util.PokerHandGenerator;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.table.redblackwar.constant.RedBlackWarConstant.Common.PAIR_MIN_LIMIT;

/**
 * 进入结算阶段
 *
 * @author 2CL
 */
public class RedBlackWarSettlementPhase extends BaseSettlementPhase<RedBlackWarGameDataVo> {

    private final RedBlackWarSampleManager redBlackWarSampleManager;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public RedBlackWarSettlementPhase(BaseTableGameController<RedBlackWarGameDataVo> gameController) {
        super(gameController);
        redBlackWarSampleManager = CommonUtil.getContext().getBean(RedBlackWarSampleManager.class);
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        //根据牌型获得牌
        List<Integer> joker = null;
        Pair<Long, Long> currentPool = canTriggerRecycling();
        if (currentPool != null) {
            List<Integer> result = generateRecyclingResults();
            if (result == null) {
                log.error("红黑大战回收触发 生成结果失败 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            } else {
                joker = result;
                log.info("红黑大战回收触发 生成结果成功 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            }
        }
        if (joker == null) {
            joker = PokerCardUtils.getPokerIntIdExceptJoker();
            Collections.shuffle(joker);
        }
        //取红方的牌
        List<Card> redCard = joker.subList(0, 3).stream().map(Card::new).collect(Collectors.toList());
        if (Objects.nonNull(gameDataVo.getRed()) && gameDataVo.getRed().size() == 3) {
            redCard = gameDataVo.getRed();
            gameDataVo.setRed(null);
        }
        Card[] redCardArr = redCard.toArray(CardComparatorUtil.SAMPLE);
        //红方牌型
        HandType redHandType = CardComparatorUtil.getCardType(redCardArr);
        //取黑方的牌
        List<Card> blackCard = joker.subList(3, 6).stream().map(Card::new).collect(Collectors.toList());
        if (Objects.nonNull(gameDataVo.getBlack()) && gameDataVo.getBlack().size() == 3) {
            blackCard = gameDataVo.getBlack();
            gameDataVo.setBlack(null);
        }
        Card[] blackCardArr = blackCard.toArray(CardComparatorUtil.SAMPLE);
        //黑方牌型
        HandType blackHandType = CardComparatorUtil.getCardType(blackCardArr);
        //比较牌大小
        int result = CardComparatorUtil.compareCards(redHandType, redCardArr, blackHandType, blackCardArr);
        //押注信息
        Map<Integer, Map<Long, List<Integer>>> betInfo = gameDataVo.getBetInfo();
        boolean luckBet;
        Map<RedBlackWarConstant.Camp, Map<HandType, List<WinPosWeightCfg>>> winMap = redBlackWarSampleManager.getWinMap();
        Map<Long, DefaultKeyValue<Long, Long>> playerGet = new HashMap<>();
        List<WinPosWeightCfg> weightCfgList;
        if (result > 0) {
            //红方胜利
            luckBet = isLuckBet(redHandType, redCard);
            weightCfgList = winMap.get(RedBlackWarConstant.Camp.RED).get(redHandType);
        } else {
            //黑方胜利
            luckBet = isLuckBet(blackHandType, blackCard);
            weightCfgList = winMap.get(RedBlackWarConstant.Camp.BLACK).get(blackHandType);
        }
        // 庄家变化的钱
        RoomBankerChangeParam changeParam = getRoomBankerChangeParam(gameDataVo.getRealPlayerAreaBetInfo());
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
        //遍历获奖位置
        for (WinPosWeightCfg cfg : weightCfgList) {
            List<Integer> betArea = cfg.getBetArea();
            BetAreaCfg betAreaCfg = redBlackWarSampleManager.getBetAreaMap().get(betArea.getFirst());
            //押注区域非幸运一击
            boolean luckCfg = betAreaCfg.getAreaID() == RedBlackWarConstant.Common.LUCK_AREA;
            if (!luckBet && luckCfg) {
                continue;
            }
            //获取押注玩家
            Map<Long, List<Integer>> betMap = betInfo.get(betAreaCfg.getId());
            if (Objects.nonNull(betMap)) {
                //计算奖励
                for (Map.Entry<Long, List<Integer>> entry : betMap.entrySet()) {
                    Long playerId = entry.getKey();
                    int totalBet = entry.getValue().stream().mapToInt(Integer::intValue).sum();
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                    if (gamePlayer == null) {
                        continue;
                    }
                    SettlementData settlementData = calcGold(gamePlayer, cfg.getOdds(), cfg.getReturnRate(), cfg, totalBet);
                    if (!settlementDataMap.containsKey(playerId)) {
                        settlementDataMap.put(playerId, settlementData);
                    } else {
                        settlementDataMap.get(playerId).increaseBySettlementData(settlementData);
                    }
                    // 给玩家添加金币
                    gameController.addItem(gamePlayer.getId(), settlementData.getTotalWin(), AddType.GAME_SETTLEMENT, gameDataVo.getRoomCfg().getId() + "");
                    DefaultKeyValue<Long, Long> keyValue = playerGet.computeIfAbsent(playerId, key -> new DefaultKeyValue<>(0L, 0L));
                    keyValue.setKey(keyValue.getKey() + totalBet);
                    keyValue.setValue(keyValue.getValue() + settlementData.getTotalWin());
                }
                if (changeParam != null) {
                    changeParam.removeArea(betAreaCfg.getId());
                }
            }
        }
        // 计算最终的bankerChangeGold
        if (changeParam != null) {
            for (Map.Entry<Long, SettlementData> entry : settlementDataMap.entrySet()) {
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(entry.getKey());
                if (gamePlayer == null || gamePlayer instanceof GameRobotPlayer) {
                    continue;
                }
                SettlementData data = entry.getValue();
                changeParam.addTotalTaxRevenue(data.getTaxation());
                changeParam.addBankerChangeGold(Math.max(0, data.getTotalGet() - data.getBetTotal()));
            }
            calculationFinalBankerChange(changeParam);
            dealRoomPool(changeParam);
            gameController.dealBankerFlowing(changeParam, settlementDataMap);
        }
        //计算所有玩家的结算信息
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : gameDataVo.getPlayerBetInfo().entrySet()) {
            SettlementData data = settlementDataMap.computeIfAbsent(entry.getKey(), key -> new SettlementData());
            data.setBetTotal(entry.getValue().values().stream()
                    .mapToLong(a -> a.stream().mapToInt(b -> b).sum())
                    .sum());
        }
        //通知
        int winState = result > 0 ? 1 : 2;
        NotifyRedBlackWarSettleInfo settleInfo = new NotifyRedBlackWarSettleInfo();
        settleInfo.winState = winState;
        settleInfo.blackCards = blackCard.stream().map(Card::getValue).toList();
        settleInfo.blackCardType = blackHandType.getRank();
        settleInfo.redCards = redCard.stream().map(Card::getValue).toList();
        settleInfo.redCardType = redHandType.getRank();
        settleInfo.playerSettleInfos = TableMessageBuilder.getPlayerSettleInfos(gameController, playerGet, gameDataVo);
        settleInfo.isLucky = luckBet;
        settleInfo.waitEndTime = gameDataVo.getPhaseEndTime();
        //记录
        addLog(gameDataVo, playerGet, settleInfo.redCards, settleInfo.blackCards);
        //更新房间记录
        updateGameHistory(gameDataVo, blackHandType, winState);
        //清除押注历史
        betInfo.clear();
        //更新结算信息
        gameDataVo.setCurrentSettleInfo(settleInfo);
        //更新记录
        for (GamePlayer gamePlayer : gameDataVo.getGamePlayerMap().values()) {
            TablePlayerGameData tableGameData = gamePlayer.getTableGameData();
            DefaultKeyValue<Long, Long> keyValue = playerGet.get(gamePlayer.getId());
            long goldCanGet = keyValue == null ? 0 : keyValue.getValue() - keyValue.getKey();
            tableGameData.addBetRecord(goldCanGet);
        }
        //发送通知
        broadcastMsgToRoom(settleInfo);
    }

    private List<Integer> generateRecyclingResults() {
        Map<Long, Map<Integer, List<Integer>>> realPlayerBetInfo = gameDataVo.getRealPlayerBetInfo();
        if (realPlayerBetInfo == null) {
            return null;
        }

        List<Integer> cardPool = PokerCardUtils.getPokerIntIdExceptJoker();
        // 尝试两种策略：0-生成带幸运牌型，1-仅生成高牌（无幸运）
        for (int strategy = 0; strategy < 2; strategy++) {
            List<Card> handA;
            List<Card> handB;

            if (strategy == 0) {
                // 策略0：随机生成幸运范围内的牌型 (Rank 2-6)
                handA = PokerHandGenerator.dealHand(HandType.getHandType(RandomUtil.randomInt(2, 7)), cardPool);
                handB = PokerHandGenerator.dealHand(HandType.getHandType(RandomUtil.randomInt(2, 7)), cardPool);
            } else {
                // 策略1：强制生成高牌
                handA = PokerHandGenerator.dealHand(HandType.HIGH_CARD, cardPool);
                handB = PokerHandGenerator.dealHand(HandType.HIGH_CARD, cardPool);
            }

            Card[] cardsA = handA.toArray(CardComparatorUtil.SAMPLE);
            HandType typeA = CardComparatorUtil.getCardType(cardsA);
            Card[] cardsB = handB.toArray(CardComparatorUtil.SAMPLE);
            HandType typeB = CardComparatorUtil.getCardType(cardsB);

            // 比较两手牌的大小
            int comparison = CardComparatorUtil.compareCards(typeA, cardsA, typeB, cardsB);
            Map<RedBlackWarConstant.Camp, Map<HandType, List<WinPosWeightCfg>>> winMap = redBlackWarSampleManager.getWinMap();
            // 轮流假设 A赢 或 B赢，校验系统损益
            for (int side = 0; side < 2; side++) {
                HandType winnerType = (side == 0) ? typeB : typeA;
                RedBlackWarConstant.Camp camp = (side == 0) ? RedBlackWarConstant.Camp.BLACK : RedBlackWarConstant.Camp.RED;

                List<WinPosWeightCfg> weightCfgList = new ArrayList<>(winMap.get(camp).get(winnerType));

                // 如果是非幸运策略，移除幸运区域的权重配置，避免产生意外赔付
                if (strategy != 0) {
                    weightCfgList.removeIf(cfg -> {
                        BetAreaCfg area = redBlackWarSampleManager.getBetAreaMap().get(cfg.getBetArea().getFirst());
                        return area.getAreaID() == RedBlackWarConstant.Common.LUCK_AREA;
                    });
                }

                Pair<Long, Long> result = getWinOrLoseResult(realPlayerBetInfo, weightCfgList);
                // 校验是否满足系统回收条件 (盈利 > 0 且 满足特定阈值)
                if (result.getFirst() > 0 && result.getFirst() >= result.getSecond()) {
                    return assembleResultList(handA, handB, comparison, side);
                }
            }
        }
        return null;
    }

    /**
     * 组装最终发牌顺序：前3张为红方，后3张为黑方
     * side: 0 代表假设黑方赢, 1 代表假设红方赢
     * comparison: >0 代表 handA > handB
     */
    private List<Integer> assembleResultList(List<Card> handA, List<Card> handB, int comparison, int side) {
        List<Integer> finalCards = new ArrayList<>(6);
        // 判定哪一手牌应该是胜者，哪一手应该是败者
        boolean aShouldWin = (side != 0); // side=1(红) -> A赢; side=0(黑) -> B赢
        boolean aIsActuallyStronger = (comparison > 0);

        List<Card> redHand = (aShouldWin == aIsActuallyStronger) ? handA : handB;
        List<Card> blackHand = (aShouldWin == aIsActuallyStronger) ? handB : handA;

        redHand.forEach(c -> finalCards.add(c.getValue()));
        blackHand.forEach(c -> finalCards.add(c.getValue()));
        return finalCards;
    }

    @Override
    public void calculationFinalBankerChange(RoomBankerChangeParam param) {
        long totalGet = 0;
        for (Map.Entry<Integer, Map<Long, Long>> entry : param.getBankerChangeMap().entrySet()) {
            long sum = entry.getValue().values().stream().mapToLong(Long::longValue).sum();
            if (entry.getKey() == RedBlackWarConstant.Common.CLIENT_LUCK_AREA) {
                //不算税收
                totalGet += sum;
            } else {
                long realGet = sum * gameDataVo.getRoomCfg().getEffectiveRatio() / 10000;
                param.addTotalTaxRevenue(sum - realGet);
                totalGet += realGet;
            }
        }
        param.addBankerChangeGold(-totalGet);
        //计算房主收益
        param.addRoomCreatorTotalIncome(calcRoomCreatorIncome(param.getTotalTaxRevenue()));
        gameDataTracker.addGameLogData("tax", param.getTotalTaxRevenue());
    }

    @Override
    public void phaseFinishAction() {
        gameDataVo.setCurrentSettleInfo(null);
    }

    private void addLog(RedBlackWarGameDataVo gameDataVo, Map<Long, DefaultKeyValue<Long, Long>> playerGet,
                        List<Integer> redCard, List<Integer> blackCard) {
        SaveLogUtil.generalLog(gameDataVo.getPlayerBetInfo(), playerGet, gameDataVo.getGamePlayerMap(), gameController);
        gameDataTracker.addGameLogData("redCard", redCard);
        gameDataTracker.addGameLogData("blackCard", blackCard);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    private void updateGameHistory(RedBlackWarGameDataVo gameDataVo, HandType blackHandType, int result) {
        RedBlackWarHistory redBlackWarHistory = new RedBlackWarHistory();
        redBlackWarHistory.cardType = blackHandType.getRank();
        redBlackWarHistory.winner = result;
        gameDataVo.addHistory(redBlackWarHistory);
    }

    /**
     * 判断是否是幸运一击
     */
    public boolean isLuckBet(HandType handType, List<Card> black) {
        if (handType == HandType.HIGH_CARD) {
            return false;
        }
        if (handType == HandType.PAIR) {
            black.sort(Comparator.comparingInt(Card::getRank));
            //取中间牌
            Card card = black.get(1);
            return card.compare(PAIR_MIN_LIMIT, false) >= 0;
        }
        return true;
    }
}
