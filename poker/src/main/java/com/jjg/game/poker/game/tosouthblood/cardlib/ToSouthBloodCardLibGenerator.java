package com.jjg.game.poker.game.tosouthblood.cardlib;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.tosouthblood.util.ToSouthBloodCardType;
import com.jjg.game.poker.game.tosouthblood.util.ToSouthBloodHandUtils;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant.*;

/**
 * 南方前进牌库无头模拟器
 * 模拟4个机器人打一局完整的牌，记录各座位总输赢倍数，生成牌库条目
 */
public class ToSouthBloodCardLibGenerator {
    private static final Logger log = LoggerFactory.getLogger(ToSouthBloodCardLibGenerator.class);

    /** 座位数量（固定4人） */
    private static final int SEAT_COUNT = 4;
    /** 每人手牌数 */
    private static final int HAND_SIZE = 13;

    /**
     * 模拟一局游戏，返回2条牌库记录（输牌+赢牌）
     *
     * @param poolId    牌池ID
     * @param moneyCfg  金钱配置
     * @return 2条ToSouthBloodCardLib，可能为空（模拟异常时）
     */
    public static List<ToSouthBloodCardLib> simulateOneGame(int poolId, SouthernMoneyCfg moneyCfg) {
        List<ToSouthBloodCardLib> result = new ArrayList<>();

        Map<Integer, PokerCard> cardListMap = PokerDataHelper.getCardListMap(poolId);
        if (cardListMap == null || cardListMap.isEmpty()) {
            log.error("牌池为空，poolId={}", poolId);
            return result;
        }

        // 1. 洗牌发牌
        List<Integer> allCardIds = new ArrayList<>(cardListMap.keySet());
        Collections.shuffle(allCardIds);

        // 4个座位的手牌（pokerPoolId列表）
        List<List<Integer>> seatCards = new ArrayList<>();
        for (int i = 0; i < SEAT_COUNT; i++) {
            List<Integer> hand = new ArrayList<>();
            for (int j = 0; j < HAND_SIZE; j++) {
                hand.add(allCardIds.get(i * HAND_SIZE + j));
            }
            seatCards.add(hand);
        }

        // 2. 检查通杀
        boolean hasInstantWin = false;
        for (int i = 0; i < SEAT_COUNT; i++) {
            List<Card> handCards = seatCards.get(i).stream().map(cardListMap::get).collect(Collectors.toList());
            Pair<Integer, List<Integer>> instantWin = ToSouthBloodHandUtils.getInstantWinCards(handCards);
            if (instantWin != null) {
                hasInstantWin = true;
                break;
            }
        }

        // 3. 计算各座位的总输赢倍数
        long[] seatMultipliers = new long[SEAT_COUNT]; // 正=赢，负=输

        if (hasInstantWin) {
            // 通杀结算：有通杀的玩家赢，其余玩家输
            calcInstantWinSettlement(seatCards, cardListMap, seatMultipliers);
        } else {
            // 正常模拟出牌 + 炸弹结算 + 最终结算
            simulatePlayAndSettle(seatCards, cardListMap, moneyCfg, seatMultipliers);
        }

        // 4. 找出最输和最赢的座位，生成2条牌库记录
        int biggestLoserSeat = 0;
        int biggestWinnerSeat = 0;
        for (int i = 1; i < SEAT_COUNT; i++) {
            if (seatMultipliers[i] < seatMultipliers[biggestLoserSeat]) {
                biggestLoserSeat = i;
            }
            if (seatMultipliers[i] > seatMultipliers[biggestWinnerSeat]) {
                biggestWinnerSeat = i;
            }
        }

        // 输牌条目：最输座位→playerCards, multiplier为负数
        if (seatMultipliers[biggestLoserSeat] < 0) {
            result.add(buildCardLib(
                    seatMultipliers[biggestLoserSeat],
                    biggestLoserSeat,
                    seatCards
            ));
        }

        // 赢牌条目：最赢座位→playerCards, multiplier为正数
        if (seatMultipliers[biggestWinnerSeat] > 0) {
            result.add(buildCardLib(
                    seatMultipliers[biggestWinnerSeat],
                    biggestWinnerSeat,
                    seatCards
            ));
        }

        return result;
    }

    /**
     * 通杀结算
     */
    private static void calcInstantWinSettlement(List<List<Integer>> seatCards,
                                                  Map<Integer, PokerCard> cardListMap,
                                                  long[] seatMultipliers) {
        // 找出所有有通杀的座位
        List<Integer> winnerSeats = new ArrayList<>();
        for (int i = 0; i < SEAT_COUNT; i++) {
            List<Card> handCards = seatCards.get(i).stream().map(cardListMap::get).collect(Collectors.toList());
            Pair<Integer, List<Integer>> instantWin = ToSouthBloodHandUtils.getInstantWinCards(handCards);
            if (instantWin != null) {
                winnerSeats.add(i);
            }
        }

        // 通杀：输家的张数*2就是倍数，赢家获得所有输家的总倍数
        for (int i = 0; i < SEAT_COUNT; i++) {
            if (winnerSeats.contains(i)) continue;
            // 输家
            int cardCount = seatCards.get(i).size();
            long loseMulti = (long) cardCount * 2 * winnerSeats.size();
            seatMultipliers[i] = -loseMulti;
            // 每个赢家获得一份
            long winPerWinner = (long) cardCount * 2;
            for (int w : winnerSeats) {
                seatMultipliers[w] += winPerWinner;
            }
        }
    }

    /**
     * 正常模拟出牌 + 炸弹结算 + 最终结算
     */
    private static void simulatePlayAndSettle(List<List<Integer>> seatCards,
                                               Map<Integer, PokerCard> cardListMap,
                                               SouthernMoneyCfg moneyCfg,
                                               long[] seatMultipliers) {
        // 复制一份手牌用于模拟出牌（会逐渐减少）
        List<List<Integer>> remainCards = new ArrayList<>();
        for (List<Integer> hand : seatCards) {
            remainCards.add(new ArrayList<>(hand));
        }

        // 找黑桃3确定首出座位
        int currentSeat = findSpade3Seat(remainCards, cardListMap);
        if (currentSeat < 0) {
            currentSeat = 0;
        }

        boolean isFirstRound = true;
        int roundLeaderSeat = currentSeat;
        List<Integer> lastPlayCardIds = null;
        int lastPlaySeatId = -1;
        Set<Integer> passedSeats = new HashSet<>();

        // 当前轮出牌记录（用于炸弹结算）
        List<SimRoundRecord> currentRoundPlays = new ArrayList<>();

        // 炸弹结算累计
        long[] bombSettlement = new long[SEAT_COUNT];

        // 模拟出牌循环
        int maxIterations = 1000; // 防死循环
        int iteration = 0;

        while (iteration++ < maxIterations) {
            // 检查是否有人出完
            boolean gameOver = false;
            for (int i = 0; i < SEAT_COUNT; i++) {
                if (remainCards.get(i).isEmpty()) {
                    gameOver = true;
                    break;
                }
            }
            if (gameOver) break;

            List<Card> handCards = remainCards.get(currentSeat).stream()
                    .map(cardListMap::get)
                    .collect(Collectors.toList());

            boolean isLeader = (roundLeaderSeat == currentSeat) && (lastPlayCardIds == null);

            List<Card> playCards = null;

            if (isLeader) {
                // 首出
                if (isFirstRound) {
                    // 必须包含黑桃3
                    Card spade3 = handCards.stream()
                            .filter(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUIT)
                            .findFirst().orElse(null);
                    if (spade3 != null) {
                        Map<Integer, List<Card>> rankMap = ToSouthBloodHandUtils.convertCardListToRankMap(handCards);
                        playCards = ToSouthBloodHandUtils.findBestPlayWithFirstCard(rankMap, spade3);
                    }
                }
                if (playCards == null) {
                    playCards = ToSouthBloodHandUtils.findBestPlay(handCards);
                }
            } else if (!passedSeats.contains(currentSeat)) {
                // 跟牌
                if (lastPlayCardIds != null) {
                    List<Card> lastCards = lastPlayCardIds.stream().map(cardListMap::get).collect(Collectors.toList());
                    playCards = ToSouthBloodHandUtils.findBestFollowPlay(handCards, lastCards);
                }
            }

            if (CollUtil.isNotEmpty(playCards)) {
                // 出牌成功
                ToSouthBloodCardType cardType = ToSouthBloodHandUtils.getCardType(playCards);
                List<Integer> playCardIds = playCards.stream()
                        .filter(c -> c instanceof PokerCard)
                        .map(c -> ((PokerCard) c).getPokerPoolId())
                        .collect(Collectors.toList());

                // 从手牌中移除已出的牌
                remainCards.get(currentSeat).removeAll(playCardIds);

                lastPlayCardIds = playCardIds;
                lastPlaySeatId = currentSeat;
                passedSeats.clear();

                // 记录出牌
                currentRoundPlays.add(new SimRoundRecord(currentSeat, cardType, playCardIds));

                isFirstRound = false;

                // 检查是否出完 — 出完后处理最后一手炸弹结算
                if (remainCards.get(currentSeat).isEmpty()) {
                    // 处理炸弹结算
                    processBombSettlement(currentSeat, currentRoundPlays, cardListMap, moneyCfg, bombSettlement);
                    break;
                }
            } else {
                // 过牌
                passedSeats.add(currentSeat);
            }

            // 找下家
            int nextSeat = findNextActiveSeat(currentSeat, remainCards, passedSeats);

            // 判断是否一轮结束
            if (nextSeat == lastPlaySeatId || nextSeat < 0) {
                // 一轮结束
                // 处理炸弹结算
                processBombSettlement(lastPlaySeatId, currentRoundPlays, cardListMap, moneyCfg, bombSettlement);

                // 新一轮
                currentRoundPlays.clear();
                roundLeaderSeat = lastPlaySeatId;
                lastPlayCardIds = null;
                passedSeats.clear();
                currentSeat = lastPlaySeatId;
            } else {
                currentSeat = nextSeat;
            }
        }

        // 最终结算：找赢家（手牌为空的座位）
        List<Integer> winnerSeats = new ArrayList<>();
        List<Integer> loserSeats = new ArrayList<>();
        for (int i = 0; i < SEAT_COUNT; i++) {
            if (remainCards.get(i).isEmpty()) {
                winnerSeats.add(i);
            } else {
                loserSeats.add(i);
            }
        }

        if (winnerSeats.isEmpty()) {
            // 没人赢（异常情况），跳过最终结算
            log.warn("模拟牌局无赢家，iterations={}", iteration);
            return;
        }

        // 计算最终结算倍数（与ToSouthBloodSettlementPhase.calSettlement一致）
        long totalWinMulti = 0;
        for (int loserSeat : loserSeats) {
            List<Card> loserHandCards = remainCards.get(loserSeat).stream()
                    .map(cardListMap::get)
                    .collect(Collectors.toList());
            int cardCount = loserHandCards.size();

            int countTwo = ToSouthBloodHandUtils.countTwo(loserHandCards);
            int countRedTwo = ToSouthBloodHandUtils.countRedTwo(loserHandCards);
            int countBlackTwo = countTwo - countRedTwo;
            int cardMulti = (cardCount == 13) ? cardCount * 2 : cardCount;
            int redTwoMulti = moneyCfg.getRemainred2();
            int blackTwoMulti = moneyCfg.getRemainblack2();
            int optimalBombMulti = ToSouthBloodHandUtils.calcOptimalBombMultiplier(
                    loserHandCards, moneyCfg.getFourkindboom1(), moneyCfg.getRemainBoom1(), moneyCfg.getFourpairsboom1());

            int totalMulti = cardMulti + countRedTwo * redTwoMulti + countBlackTwo * blackTwoMulti + optimalBombMulti;

            // 输家倍数（乘以赢家人数）
            long loseMulti = (long) totalMulti * winnerSeats.size();
            seatMultipliers[loserSeat] -= loseMulti;
            totalWinMulti += totalMulti;
        }

        // 赢家获得总倍数
        for (int winnerSeat : winnerSeats) {
            seatMultipliers[winnerSeat] += totalWinMulti;
        }

        // 加上炸弹结算的累计
        for (int i = 0; i < SEAT_COUNT; i++) {
            seatMultipliers[i] += bombSettlement[i];
        }
    }

    /**
     * 模拟炸弹结算（简化版，复用ToSouthBloodGameController的核心逻辑）
     */
    private static void processBombSettlement(int winnerSeatId,
                                               List<SimRoundRecord> plays,
                                               Map<Integer, PokerCard> cardMap,
                                               SouthernMoneyCfg moneyCfg,
                                               long[] bombSettlement) {
        if (CollUtil.isEmpty(plays)) return;

        SimRoundRecord lastPlay = plays.getLast();
        if (lastPlay.seatId != winnerSeatId) return;
        if (!isBomb(lastPlay.cardType)) return;

        // 从后往前找炸弹链
        List<SimRoundRecord> bombChain = new ArrayList<>();
        int victimIndex = -1;
        for (int i = plays.size() - 1; i >= 0; i--) {
            SimRoundRecord record = plays.get(i);
            if (isBomb(record.cardType)) {
                bombChain.addFirst(record);
            } else {
                victimIndex = i;
                break;
            }
        }

        if (bombChain.isEmpty()) return;
        // 首出炸弹且无人压制
        if (victimIndex == -1 && bombChain.size() == 1) return;

        // 检查被炸的牌是否可被炸
        if (victimIndex != -1) {
            SimRoundRecord victimRecord = plays.get(victimIndex);
            List<Card> victimCards = victimRecord.cards.stream()
                    .map(id -> (Card) cardMap.get(id))
                    .collect(Collectors.toList());
            boolean allRank2 = !victimCards.isEmpty() && victimCards.stream().allMatch(c -> c.getRank() == RANK_2);
            boolean bombable = isBomb(victimRecord.cardType) || allRank2;
            if (!bombable) return;
        }

        // 收集需要结算的记录
        List<SimRoundRecord> settledRecords = new ArrayList<>();
        if (victimIndex != -1) {
            for (int i = victimIndex; i >= 0; i--) {
                SimRoundRecord record = plays.get(i);
                List<Card> cards = record.cards.stream()
                        .map(id -> (Card) cardMap.get(id))
                        .collect(Collectors.toList());
                boolean allRank2 = !cards.isEmpty() && cards.stream().allMatch(c -> c.getRank() == RANK_2);
                if (allRank2) {
                    settledRecords.addFirst(record);
                } else {
                    break;
                }
            }
        }
        if (bombChain.size() > 1) {
            settledRecords.addAll(bombChain.subList(0, bombChain.size() - 1));
        }

        if (CollUtil.isNotEmpty(settledRecords)) {
            long totalMultiplier = 0;
            for (SimRoundRecord settled : settledRecords) {
                long multi = getBombMultiplier(settled, cardMap, moneyCfg);
                if (multi > 0) totalMultiplier += multi;
            }

            int loserSeat;
            if (bombChain.size() >= 2) {
                loserSeat = bombChain.get(bombChain.size() - 2).seatId;
            } else {
                SimRoundRecord directVictim = victimIndex >= 0 ? plays.get(victimIndex) : settledRecords.getFirst();
                loserSeat = directVictim.seatId;
            }

            if (totalMultiplier > 0 && loserSeat != winnerSeatId) {
                bombSettlement[loserSeat] -= totalMultiplier;
                bombSettlement[winnerSeatId] += totalMultiplier;
            }
        }
    }

    /**
     * 计算被炸牌型的赔付倍数（与ToSouthBloodGameController.getBombSettlementMultiplier一致）
     */
    private static long getBombMultiplier(SimRoundRecord record,
                                           Map<Integer, PokerCard> cardMap,
                                           SouthernMoneyCfg moneyCfg) {
        List<Card> cards = record.cards.stream()
                .map(id -> (Card) cardMap.get(id))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(cards)) return 0;

        cards.sort(ToSouthBloodHandUtils.CARD_COMPARATOR);

        boolean allRank2 = cards.stream().allMatch(c -> c.getRank() == RANK_2);
        if (allRank2 && (record.cardType == ToSouthBloodCardType.SINGLE
                || record.cardType == ToSouthBloodCardType.PAIR
                || record.cardType == ToSouthBloodCardType.TRIPLE)) {
            long multi = 0;
            for (Card c : cards) {
                boolean isRed = c.getSuit() == HEART_SUIT || c.getSuit() == DIAMOND_SUIT;
                multi += isRed ? moneyCfg.getRemainred2() : moneyCfg.getRemainblack2();
            }
            return multi;
        }

        if (record.cardType == ToSouthBloodCardType.BOMB_QUAD) {
            return moneyCfg.getFourkindboom();
        }
        if (record.cardType == ToSouthBloodCardType.CONSECUTIVE_PAIRS) {
            return cards.size() >= 8 ? moneyCfg.getFourpairsboom() : moneyCfg.getRemainBoom();
        }
        return 0;
    }

    /**
     * 找黑桃3所在的座位
     */
    private static int findSpade3Seat(List<List<Integer>> seatCards, Map<Integer, PokerCard> cardMap) {
        for (int i = 0; i < SEAT_COUNT; i++) {
            for (int cardId : seatCards.get(i)) {
                PokerCard card = cardMap.get(cardId);
                if (card != null && card.getRank() == RANK_3 && card.getSuit() == SPADE_SUIT) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 找下一个活跃座位（手牌非空、未pass的）
     */
    private static int findNextActiveSeat(int currentSeat, List<List<Integer>> remainCards, Set<Integer> passedSeats) {
        for (int i = 1; i < SEAT_COUNT; i++) {
            int nextSeat = (currentSeat + i) % SEAT_COUNT;
            if (!remainCards.get(nextSeat).isEmpty() && !passedSeats.contains(nextSeat)) {
                return nextSeat;
            }
        }
        return -1;
    }

    private static boolean isBomb(ToSouthBloodCardType type) {
        return type == ToSouthBloodCardType.BOMB_QUAD || type == ToSouthBloodCardType.CONSECUTIVE_PAIRS;
    }

    /**
     * 构建牌库条目
     */
    private static ToSouthBloodCardLib buildCardLib(long multiplier, int targetSeat,
                                                   List<List<Integer>> seatCards) {
        List<Integer> playerCards = new ArrayList<>(seatCards.get(targetSeat));
        List<List<Integer>> robotCards = new ArrayList<>();
        for (int i = 0; i < SEAT_COUNT; i++) {
            if (i != targetSeat) {
                robotCards.add(new ArrayList<>(seatCards.get(i)));
            }
        }
        return new ToSouthBloodCardLib(multiplier, playerCards, robotCards);
    }

    /**
     * 模拟出牌记录
     */
    private static class SimRoundRecord {
        int seatId;
        ToSouthBloodCardType cardType;
        List<Integer> cards; // pokerPoolId

        SimRoundRecord(int seatId, ToSouthBloodCardType cardType, List<Integer> cards) {
            this.seatId = seatId;
            this.cardType = cardType;
            this.cards = cards;
        }
    }
}
