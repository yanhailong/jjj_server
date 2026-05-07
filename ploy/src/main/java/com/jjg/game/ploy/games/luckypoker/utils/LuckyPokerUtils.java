package com.jjg.game.ploy.games.luckypoker.utils;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.utils.PokerCardUtils.EPokerSuit;
import com.jjg.game.ploy.data.PloyCard;
import com.jjg.game.ploy.games.luckypoker.data.PokerRank;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2026/3/20
 */
public class LuckyPokerUtils {
    private static final int HAND_SIZE = 5;
    //点数
    private static final List<Integer> POINTS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
    //花色
    private static final List<EPokerSuit> SUITS = List.of(EPokerSuit.DIAMOND, EPokerSuit.CLUBS, EPokerSuit.HEART, EPokerSuit.SPADES);
    private static final Set<Integer> ROYAL_RANKS = Set.of(1, 10, 11, 12, 13);
    private static final List<PloyCard> FULL_DECK = buildFullDeck();

    /**
     * 基于当前五张手牌，给客户端生成建议保留的牌。
     */
    public static List<PloyCard> suggestSavePokerIds(List<PloyCard> cards) {
        // 优先保留已成型或最接近奖励牌型的组合，最后才退化为保留当前最大牌。
        List<PloyCard> suggestCards = findStrongMadeHand(cards);
        if (suggestCards.isEmpty()) {
            suggestCards = findFourToRoyalFlush(cards);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findThreeToRoyalFlush(cards);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findBestStraightDraw(cards, true);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findFourToFlush(cards);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findBestStraightDraw(cards, false);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findNOfAKind(cards, 3);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findTwoPair(cards);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findPair(cards, true);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findPair(cards, false);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = findSuitedHighCards(cards);
        }
        if (suggestCards.isEmpty()) {
            suggestCards = List.of(findHighestCard(cards));
        }

        return suggestCards;
    }

    /**
     * 基于当前局已经生成的首手牌、补牌序列和赔率表，给出这局的最优保留建议。
     */
    public static List<PloyCard> suggestSavePokerIds(List<PloyCard> firstCards, List<PloyCard> secondCards, Map<Integer, Integer> odds) {
        return suggestSavePokerIds(firstCards);
    }

    /**
     * 根据传入的牌列表，检查是什么牌型
     *
     * @param cards
     * @return
     */
    public static PokerRank checkPokerRank(List<PloyCard> cards) {
        List<Integer> ranks = new ArrayList<>();
        int firstSuit = cards.get(0).getSuit();
        boolean isFlush = true;
        for (PloyCard card : cards) {
            ranks.add(card.getRank());
            if (card.getSuit() != firstSuit) {
                isFlush = false;
            }
        }
        boolean isStraight = isStraightPoints(ranks);

        if (isFlush && isRoyalStraightPoints(ranks)) return PokerRank.ROYAL_FLUSH;
        if (isFlush && isStraight) return PokerRank.STRAIGHT_FLUSH;

        // 统计点数频次
        Map<Integer, Integer> freq = new HashMap<>();
        for (int rank : ranks) {
            freq.merge(rank, 1, Integer::sum);
        }
        List<Integer> counts = new ArrayList<>(freq.values());
        counts.sort(Collections.reverseOrder());

        if (counts.get(0) == 4) return PokerRank.FOUR_OF_A_KIND;
        if (counts.get(0) == 3 && counts.get(1) == 2) return PokerRank.FULL_HOUSE;
        if (isFlush) return PokerRank.FLUSH;
        if (isStraight) return PokerRank.STRAIGHT;
        if (counts.get(0) == 3) return PokerRank.THREE_OF_A_KIND;
        if (counts.get(0) == 2 && counts.get(1) == 2) return PokerRank.TWO_PAIR;
        if (counts.get(0) == 2) {
            // 检查对子是否是J或更大（J/Q/K/A）
            for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
                if (entry.getValue() == 2) {
                    int pairRank = entry.getKey();
                    if (pairRank == 1 || pairRank >= 11) {
                        return PokerRank.ONE_PAIR_OR_BETTER;
                    }
                }
            }
        }
        return PokerRank.HIGH_CARD;
    }

    /**
     * 根据牌型类型获取对应的5张牌id列表
     *
     * @param rank 牌型
     * @return 5张牌id列表
     */
    public static List<PloyCard> getCardIdsByRank(PokerRank rank) {
        return switch (rank) {
            case HIGH_CARD -> generateHighCard();
            case ONE_PAIR_OR_BETTER -> generateOnePair();
            case TWO_PAIR -> generateTwoPair();
            case THREE_OF_A_KIND -> generateThreeOfAKind();
            case STRAIGHT -> generateStraight();
            case FLUSH -> generateFlush();
            case FULL_HOUSE -> generateFullHouse();
            case FOUR_OF_A_KIND -> generateFourOfAKind();
            case STRAIGHT_FLUSH -> generateStraightFlush();
            case ROYAL_FLUSH -> generateRoyalFlush();
        };
    }

    /**
     * 根据牌型类型获取对应的5张牌id列表
     *
     * @param ranks 牌型列表
     * @return
     */
    public static List<List<PloyCard>> getCardIdsByRanks(List<PokerRank> ranks) {
        List<List<PloyCard>> result = new ArrayList<>();
        Set<Integer> usedCards = new HashSet<>();
        for (PokerRank rank : ranks) {
            List<PloyCard> hand;
            int attempts = 0;
            do {
                hand = getCardIdsByRank(rank);
                attempts++;
                if (attempts > 1000) {
                    throw new RuntimeException("Unable to generate non-overlapping hand for rank: " + rank);
                }
            } while (hasConflict(hand, usedCards));
            for (PloyCard card : hand) {
                usedCards.add(cardKey(card));
            }
            result.add(hand);
        }
        return result;
    }

    /**
     * 将cards对象列表转为id列表
     *
     * @param cards
     * @return
     */
    public static List<Integer> card2Ids(List<PloyCard> cards) {
        return cards.stream().map(PloyCard::getClientCardId).collect(Collectors.toList());
    }

    private static boolean hasConflict(List<PloyCard> hand, Set<Integer> usedCards) {
        Set<Integer> handKeys = new HashSet<>();
        for (PloyCard card : hand) {
            int key = cardKey(card);
            if (usedCards.contains(key) || !handKeys.add(key)) {
                return true;
            }
        }
        return false;
    }

    private static int cardKey(PloyCard card) {
        return card.getSuit() * 13 + card.getRank();
    }

    /**
     * 查找已经成型且值得直接保留的强牌组合。
     */
    private static List<PloyCard> findStrongMadeHand(List<PloyCard> cards) {
        PokerRank pokerRank = checkPokerRank(cards);
        return switch (pokerRank) {
            case STRAIGHT, FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH, ROYAL_FLUSH -> cards;
            default -> Collections.emptyList();
        };
    }

    /**
     * 查找指定张数的相同点数组合，并优先返回点数最大的那组。
     */
    private static List<PloyCard> findNOfAKind(List<PloyCard> cards, int count) {
        Map<Integer, List<PloyCard>> cardsByRank = groupByRank(cards);
        List<PloyCard> best = Collections.emptyList();
        int bestRank = Integer.MIN_VALUE;
        for (Map.Entry<Integer, List<PloyCard>> entry : cardsByRank.entrySet()) {
            if (entry.getValue().size() == count && rankValue(entry.getKey()) > bestRank) {
                best = new ArrayList<>(entry.getValue());
                bestRank = rankValue(entry.getKey());
            }
        }
        return best;
    }

    /**
     * 查找两对组合，存在时返回两组对子对应的四张牌。
     */
    private static List<PloyCard> findTwoPair(List<PloyCard> cards) {
        Map<Integer, List<PloyCard>> cardsByRank = groupByRank(cards);
        List<PloyCard> result = new ArrayList<>();
        int pairCount = 0;
        for (List<PloyCard> rankCards : cardsByRank.values()) {
            if (rankCards.size() == 2) {
                pairCount++;
                result.addAll(rankCards);
            }
        }
        return pairCount == 2 ? result : Collections.emptyList();
    }

    /**
     * 查找对子，可按是否只接受大对子进行过滤。
     */
    private static List<PloyCard> findPair(List<PloyCard> cards, boolean needHighPair) {
        Map<Integer, List<PloyCard>> cardsByRank = groupByRank(cards);
        List<PloyCard> best = Collections.emptyList();
        int bestRank = Integer.MIN_VALUE;
        for (Map.Entry<Integer, List<PloyCard>> entry : cardsByRank.entrySet()) {
            if (entry.getValue().size() != 2 || isHighRank(entry.getKey()) != needHighPair) {
                continue;
            }
            if (rankValue(entry.getKey()) > bestRank) {
                best = new ArrayList<>(entry.getValue());
                bestRank = rankValue(entry.getKey());
            }
        }
        return best;
    }

    /**
     * 查找四张同花且同时属于皇家同花顺点数范围的组合。
     */
    private static List<PloyCard> findFourToRoyalFlush(List<PloyCard> cards) {
        List<PloyCard> best = Collections.emptyList();
        int bestScore = Integer.MIN_VALUE;
        for (List<PloyCard> suitCards : groupBySuit(cards).values()) {
            List<PloyCard> royalCards = new ArrayList<>();
            for (PloyCard card : suitCards) {
                if (ROYAL_RANKS.contains(card.getRank())) {
                    royalCards.add(card);
                }
            }
            if (royalCards.size() < 4) {
                continue;
            }
            List<PloyCard> candidate = takeTopByRank(royalCards, 4);
            int score = cardStrength(candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 查找四张同花组合，并优先返回高张价值更高的一组。
     */
    private static List<PloyCard> findFourToFlush(List<PloyCard> cards) {
        List<PloyCard> best = Collections.emptyList();
        int bestScore = Integer.MIN_VALUE;
        for (List<PloyCard> suitCards : groupBySuit(cards).values()) {
            if (suitCards.size() < 4) {
                continue;
            }
            List<PloyCard> candidate = takeTopByRank(suitCards, 4);
            int score = countHighCards(candidate) * 100 + cardStrength(candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 查找三张同花且属于皇家同花顺点数范围的组合。
     */
    private static List<PloyCard> findThreeToRoyalFlush(List<PloyCard> cards) {
        List<PloyCard> best = Collections.emptyList();
        int bestScore = Integer.MIN_VALUE;
        for (List<PloyCard> suitCards : groupBySuit(cards).values()) {
            List<PloyCard> royalCards = new ArrayList<>();
            for (PloyCard card : suitCards) {
                if (ROYAL_RANKS.contains(card.getRank())) {
                    royalCards.add(card);
                }
            }
            if (royalCards.size() < 3) {
                continue;
            }
            List<PloyCard> candidate = takeTopByRank(royalCards, 3);
            int score = cardStrength(candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 查找同花高张组合，用于没有明显成型牌时保留潜力牌。
     */
    private static List<PloyCard> findSuitedHighCards(List<PloyCard> cards) {
        List<PloyCard> best = Collections.emptyList();
        int bestScore = Integer.MIN_VALUE;
        for (List<PloyCard> suitCards : groupBySuit(cards).values()) {
            List<PloyCard> highCards = new ArrayList<>();
            for (PloyCard card : suitCards) {
                if (isHighRank(card.getRank())) {
                    highCards.add(card);
                }
            }
            if (highCards.size() < 2) {
                continue;
            }
            List<PloyCard> candidate = takeTopByRank(highCards, Math.min(2, highCards.size()));
            int score = cardStrength(candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 查找最优的三张或四张顺子听牌，可选是否要求同花。
     */
    private static List<PloyCard> findBestStraightDraw(List<PloyCard> cards, boolean needSameSuit) {
        List<PloyCard> best = Collections.emptyList();
        int bestScore = Integer.MIN_VALUE;

        List<List<PloyCard>> groupList = new ArrayList<>();
        groupList.addAll(getSubsets(cards, 3));
        groupList.addAll(getSubsets(cards, 4));

        for (List<PloyCard> candidate : groupList) {
            if (needSameSuit && !isSameSuit(candidate)) {
                continue;
            }
            int completionCount = countStraightCompletionRanks(candidate);
            if (completionCount < 1) {
                continue;
            }
            // 可补成顺子的点数越多，听牌价值越高；高张数量和牌力用于同类场景下的细分排序。
            int score = completionCount * 1000 + countHighCards(candidate) * 100 + cardStrength(candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static List<List<PloyCard>> getSubsets(List<PloyCard> cards, int size) {
        List<List<PloyCard>> result = new ArrayList<>();
        buildSubsets(cards, size, 0, new ArrayList<>(), result);
        return result;
    }

    private static List<PloyCard> extractHoldCards(List<PloyCard> cards, int mask) {
        List<PloyCard> result = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                result.add(cards.get(i));
            }
        }
        return result;
    }

    private static double calculateExpectedRankValue(List<PloyCard> holdCards, List<PloyCard> remainingDeck) {
        int drawCount = HAND_SIZE - holdCards.size();
        if (drawCount == 0) {
            return checkPokerRank(holdCards).rank;
        }

        long[] sumAndCount = new long[2];
        List<PloyCard> currentHand = new ArrayList<>(HAND_SIZE);
        currentHand.addAll(holdCards);
        enumerateDrawCombinations(remainingDeck, 0, drawCount, currentHand, sumAndCount);
        return (double) sumAndCount[0] / sumAndCount[1];
    }

    private static void enumerateDrawCombinations(List<PloyCard> remainingDeck, int start, int drawCount, List<PloyCard> currentHand, long[] sumAndCount) {
        if (drawCount == 0) {
            sumAndCount[0] += checkPokerRank(currentHand).rank;
            sumAndCount[1]++;
            return;
        }

        for (int i = start; i <= remainingDeck.size() - drawCount; i++) {
            currentHand.add(remainingDeck.get(i));
            enumerateDrawCombinations(remainingDeck, i + 1, drawCount - 1, currentHand, sumAndCount);
            currentHand.remove(currentHand.size() - 1);
        }
    }

    private static List<PloyCard> buildRemainingDeck(List<PloyCard> cards) {
        Set<Integer> usedCardIds = new HashSet<>();
        for (PloyCard card : cards) {
            usedCardIds.add(card.getClientCardId());
        }

        List<PloyCard> remainingDeck = new ArrayList<>(FULL_DECK.size() - cards.size());
        for (PloyCard card : FULL_DECK) {
            if (!usedCardIds.contains(card.getClientCardId())) {
                remainingDeck.add(card);
            }
        }
        return remainingDeck;
    }

    private static List<PloyCard> buildFullDeck() {
        List<PloyCard> deck = new ArrayList<>(SUITS.size() * POINTS.size());
        for (EPokerSuit suit : SUITS) {
            for (int point : POINTS) {
                deck.add(new PloyCard(suit, point));
            }
        }
        return Collections.unmodifiableList(deck);
    }

    private static List<PloyCard> buildFinalCards(List<PloyCard> holdCards, List<PloyCard> secondCards, int handSize) {
        List<PloyCard> result = new ArrayList<>(handSize);
        result.addAll(holdCards);
        int needDealCount = handSize - holdCards.size();
        if (needDealCount > 0) {
            result.addAll(secondCards.subList(0, needDealCount));
        }
        return result;
    }

    private static int getPayout(PokerRank pokerRank, Map<Integer, Integer> odds) {
        if (odds == null || odds.isEmpty()) {
            return pokerRank.rank;
        }
        return odds.getOrDefault(pokerRank.rank, 0);
    }

    private static void buildSubsets(List<PloyCard> cards, int size, int start, List<PloyCard> current, List<List<PloyCard>> result) {
        if (current.size() == size) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i <= cards.size() - (size - current.size()); i++) {
            current.add(cards.get(i));
            buildSubsets(cards, size, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static int countStraightCompletionRanks(List<PloyCard> cards) {
        Set<Integer> ranks = new HashSet<>();
        for (PloyCard card : cards) {
            if (!ranks.add(card.getRank())) {
                return 0;
            }
        }

        // 统计这四张牌再补任意一张后，有多少种点数能组成顺子。
        int result = 0;
        for (int rank : POINTS) {
            if (ranks.contains(rank)) {
                continue;
            }
            List<Integer> points = new ArrayList<>(ranks);
            points.add(rank);
            if (isStraightPoints(points)) {
                result++;
            }
        }
        return result;
    }

    private static boolean isSameSuit(List<PloyCard> cards) {
        int suit = cards.getFirst().getSuit();
        for (PloyCard card : cards) {
            if (card.getSuit() != suit) {
                return false;
            }
        }
        return true;
    }

    private static List<PloyCard> takeTopByRank(List<PloyCard> cards, int limit) {
        List<PloyCard> sorted = new ArrayList<>(cards);
        sorted.sort((left, right) -> Integer.compare(rankValue(right.getRank()), rankValue(left.getRank())));
        return new ArrayList<>(sorted.subList(0, Math.min(limit, sorted.size())));
    }

    private static int countHighCards(List<PloyCard> cards) {
        int count = 0;
        for (PloyCard card : cards) {
            if (isHighRank(card.getRank())) {
                count++;
            }
        }
        return count;
    }

    private static int cardStrength(List<PloyCard> cards) {
        int result = 0;
        for (PloyCard card : cards) {
            result += rankValue(card.getRank());
        }
        return result;
    }

    private static boolean isHighRank(int rank) {
        return rank == 1 || rank >= 11;
    }

    private static int rankValue(int rank) {
        return rank == 1 ? 14 : rank;
    }

    private static Map<Integer, List<PloyCard>> groupByRank(List<PloyCard> cards) {
        Map<Integer, List<PloyCard>> result = new HashMap<>();
        for (PloyCard card : cards) {
            result.computeIfAbsent(card.getRank(), key -> new ArrayList<>()).add(card);
        }
        return result;
    }

    private static Map<Integer, List<PloyCard>> groupBySuit(List<PloyCard> cards) {
        Map<Integer, List<PloyCard>> result = new HashMap<>();
        for (PloyCard card : cards) {
            result.computeIfAbsent(card.getSuit(), key -> new ArrayList<>()).add(card);
        }
        return result;
    }

    /**
     * 查找当前手牌中的最大单张。
     */
    private static PloyCard findHighestCard(List<PloyCard> cards) {
        PloyCard best = cards.getFirst();
        for (int i = 1; i < cards.size(); i++) {
            PloyCard current = cards.get(i);
            if (current.compare(best) > 0) {
                best = current;
            }
        }
        return best;
    }

    /**
     * 散牌：5张不同点数，非顺子，非同花
     */
    private static List<PloyCard> generateHighCard() {
        List<Integer> points = pickDistinctPoints(5);
        while (isStraightPoints(points)) {
            points = pickDistinctPoints(5);
        }
        List<EPokerSuit> suits = pickNonFlushSuits(5);
        List<PloyCard> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            result.add(new PloyCard(suits.get(i), points.get(i)));
        }
        return result;
    }

    /**
     * 一对J或更大：一对J/Q/K/A + 3张不同点数散牌
     */
    private static List<PloyCard> generateOnePair() {
        int[] bigPairPoints = {1, 11, 12, 13};
        int pairPoint = bigPairPoints[RandomUtils.randomInt(bigPairPoints.length)];
        List<EPokerSuit> pairSuits = randomSuits(2);
        List<Integer> kickerPoints = pickDistinctPointsExcluding(3, Set.of(pairPoint));

        List<PloyCard> result = new ArrayList<>();
        result.add(new PloyCard(pairSuits.get(0), pairPoint));
        result.add(new PloyCard(pairSuits.get(1), pairPoint));
        for (int i = 0; i < 3; i++) {
            result.add(new PloyCard(randomSuit(), kickerPoints.get(i)));
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * 两对：2组对子 + 1张散牌
     */
    private static List<PloyCard> generateTwoPair() {
        List<Integer> pairPoints = pickDistinctPoints(2);
        int pair1 = pairPoints.get(0);
        int pair2 = pairPoints.get(1);
        int kicker = pickDistinctPointsExcluding(1, Set.of(pair1, pair2)).get(0);

        List<EPokerSuit> suits1 = randomSuits(2);
        List<EPokerSuit> suits2 = randomSuits(2);
        List<PloyCard> result = new ArrayList<>();
        result.add(new PloyCard(suits1.get(0), pair1));
        result.add(new PloyCard(suits1.get(1), pair1));
        result.add(new PloyCard(suits2.get(0), pair2));
        result.add(new PloyCard(suits2.get(1), pair2));
        result.add(new PloyCard(randomSuit(), kicker));
        Collections.shuffle(result);
        return result;
    }

    /**
     * 三张：3张相同点数 + 2张不同散牌
     */
    private static List<PloyCard> generateThreeOfAKind() {
        int triplePoint = RandomUtils.randomMinMax(1, 13);
        List<EPokerSuit> tripleSuits = randomSuits(3);
        List<Integer> kickerPoints = pickDistinctPointsExcluding(2, Set.of(triplePoint));

        List<PloyCard> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            result.add(new PloyCard(tripleSuits.get(i), triplePoint));
        }
        result.add(new PloyCard(randomSuit(), kickerPoints.get(0)));
        result.add(new PloyCard(randomSuit(), kickerPoints.get(1)));
        Collections.shuffle(result);
        return result;
    }

    /**
     * 顺子：5张连续点数，非同花
     */
    private static List<PloyCard> generateStraight() {
        List<Integer> points = randomStraightPoints();
        List<EPokerSuit> suits = pickNonFlushSuits(5);
        List<PloyCard> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            result.add(new PloyCard(suits.get(i), points.get(i)));
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * 同花：5张同花色，非顺子
     */
    private static List<PloyCard> generateFlush() {
        EPokerSuit suit = randomSuit();
        List<Integer> points = pickDistinctPoints(5);
        while (isStraightPoints(points)) {
            points = pickDistinctPoints(5);
        }
        List<PloyCard> result = new ArrayList<>();
        for (int point : points) {
            result.add(new PloyCard(suit, point));
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * 葫芦：3张相同 + 2张相同（不同点数）
     */
    private static List<PloyCard> generateFullHouse() {
        List<Integer> twoPoints = pickDistinctPoints(2);
        int triplePoint = twoPoints.get(0);
        int pairPoint = twoPoints.get(1);

        List<EPokerSuit> tripleSuits = randomSuits(3);
        List<EPokerSuit> pairSuits = randomSuits(2);
        List<PloyCard> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            result.add(new PloyCard(tripleSuits.get(i), triplePoint));
        }
        for (int i = 0; i < 2; i++) {
            result.add(new PloyCard(pairSuits.get(i), pairPoint));
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * 四条：4张相同点数 + 1张散牌
     */
    private static List<PloyCard> generateFourOfAKind() {
        int quadPoint = RandomUtils.randomMinMax(1, 13);
        int kicker = pickDistinctPointsExcluding(1, Set.of(quadPoint)).get(0);

        List<PloyCard> result = new ArrayList<>();
        for (EPokerSuit suit : SUITS) {
            result.add(new PloyCard(suit, quadPoint));
        }
        result.add(new PloyCard(randomSuit(), kicker));
        Collections.shuffle(result);
        return result;
    }

    /**
     * 同花顺：同花色且连续，排除皇家同花顺
     */
    private static List<PloyCard> generateStraightFlush() {
        EPokerSuit suit = randomSuit();
        List<Integer> points = randomStraightPoints();
        while (isRoyalStraightPoints(points)) {
            points = randomStraightPoints();
        }
        List<PloyCard> result = new ArrayList<>();
        for (int point : points) {
            result.add(new PloyCard(suit, point));
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * 皇家同花顺：10-J-Q-K-A 同花色
     */
    private static List<PloyCard> generateRoyalFlush() {
        EPokerSuit suit = randomSuit();
        List<PloyCard> result = new ArrayList<>();
        result.add(new PloyCard(suit, 1));
        result.add(new PloyCard(suit, 10));
        result.add(new PloyCard(suit, 11));
        result.add(new PloyCard(suit, 12));
        result.add(new PloyCard(suit, 13));
        Collections.shuffle(result);
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 随机选n个不同的点数(1-13)
     */
    private static List<Integer> pickDistinctPoints(int n) {
        List<Integer> allPoints = new ArrayList<>(POINTS);
        Collections.shuffle(allPoints);
        return new ArrayList<>(allPoints.subList(0, n));
    }

    /**
     * 随机选n个不同的点数，排除指定点数
     */
    private static List<Integer> pickDistinctPointsExcluding(int n, Set<Integer> exclude) {
        List<Integer> available = new ArrayList<>();
        for (int point : POINTS) {
            if (!exclude.contains(point)) {
                available.add(point);
            }
        }
        Collections.shuffle(available);
        return new ArrayList<>(available.subList(0, n));
    }

    private static EPokerSuit randomSuit() {
        return SUITS.get(RandomUtils.randomInt(SUITS.size()));
    }

    /**
     * 随机选n个不同的花色
     */
    private static List<EPokerSuit> randomSuits(int n) {
        List<EPokerSuit> shuffled = new ArrayList<>(SUITS);
        Collections.shuffle(shuffled);
        return new ArrayList<>(shuffled.subList(0, n));
    }

    /**
     * 生成n个花色，确保不全是同花色
     */
    private static List<EPokerSuit> pickNonFlushSuits(int n) {
        List<EPokerSuit> suits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            suits.add(randomSuit());
        }
        EPokerSuit first = suits.get(0);
        boolean allSame = suits.stream().allMatch(s -> s == first);
        if (allSame) {
            int idx = RandomUtils.randomInt(n);
            EPokerSuit different;
            do {
                different = randomSuit();
            } while (different == first);
            suits.set(idx, different);
        }
        return suits;
    }

    /**
     * 随机生成顺子的5个连续点数
     * A-2-3-4-5, 2-3-4-5-6, ..., 9-10-J-Q-K, 10-J-Q-K-A
     */
    private static List<Integer> randomStraightPoints() {
        int start = RandomUtils.randomMinMax(1, 10);
        List<Integer> points = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            points.add((start - 1 + i) % 13 + 1);
        }
        return points;
    }

    /**
     * 判断5个点数是否构成顺子
     */
    private static boolean isStraightPoints(List<Integer> points) {
        // A高顺子: 1,10,11,12,13（仅 5 张点数时适用）
        if (points.size() != 5) {
            return false;
        }
        List<Integer> sorted = new ArrayList<>(points);
        Collections.sort(sorted);
        // 普通顺子
        boolean normal = true;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i) - sorted.get(i - 1) != 1) {
                normal = false;
                break;
            }
        }
        if (normal) {
            return true;
        }
        return sorted.get(0) == 1 && sorted.get(1) == 10
                && sorted.get(2) == 11 && sorted.get(3) == 12 && sorted.get(4) == 13;
    }

    /**
     * 判断是否是皇家同花顺的点数 (10,J,Q,K,A)
     */
    private static boolean isRoyalStraightPoints(List<Integer> points) {
        Set<Integer> set = new HashSet<>(points);
        return set.contains(1) && set.contains(10) && set.contains(11)
                && set.contains(12) && set.contains(13);
    }

}
