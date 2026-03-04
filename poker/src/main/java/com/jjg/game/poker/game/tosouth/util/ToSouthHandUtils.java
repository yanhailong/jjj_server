package com.jjg.game.poker.game.tosouth.util;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.*;
import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.InstantWinType.*;

/**
 * 南方前进牌型工具类
 */
public class ToSouthHandUtils {

    /**
     * 牌点数排序：2 > A > K > ... > 3
     * 花色排序：红心 > 方块 > 梅花 > 黑桃
     */
    public static final Comparator<Card> CARD_COMPARATOR = (c1, c2) -> {
        if (c1 == null && c2 == null) return 0;
        if (c1 == null) return 1;
        if (c2 == null) return -1;
        int rank1 = c1.getRank();
        int rank2 = c2.getRank();
        if (rank1 != rank2) {
            return Integer.compare(rank2, rank1); // 降序
        }
        return Integer.compare(c1.getSuit(), c2.getSuit()); // 花色降序
    };
    private static final Logger log = LoggerFactory.getLogger(ToSouthHandUtils.class);

    public static String cardListToString(List<Card> cards) {
        if (cards == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cards.size(); i++) {
            sb.append(cardToString(cards.get(i)));
            if (i < cards.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 判断牌型
     */
    public static ToSouthCardType getCardType(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return ToSouthCardType.NONE;
        int size = cards.size();
        cards.sort(CARD_COMPARATOR);

        if (size == 1) return ToSouthCardType.SINGLE;
        if (size == 2 && isPair(cards)) return ToSouthCardType.PAIR;
        if (size == 3 && isTriple(cards)) return ToSouthCardType.TRIPLE;
        if (size == 4 && isBombQuad(cards)) return ToSouthCardType.BOMB_QUAD; // 四张是炸弹
        if (size >= 3 && isStraight(cards)) return ToSouthCardType.STRAIGHT;
        if (size >= 6 && size % 2 == 0 && isConsecutivePairs(cards)) return ToSouthCardType.CONSECUTIVE_PAIRS;

        return ToSouthCardType.NONE;
    }

    private static boolean isPair(List<Card> cards) {
        return cards.get(0).getRank() == cards.get(1).getRank();
    }

    private static boolean isTriple(List<Card> cards) {
        return cards.get(0).getRank() == cards.get(1).getRank() && cards.get(1).getRank() == cards.get(2).getRank();
    }

    private static boolean isBombQuad(List<Card> cards) {
        return cards.get(0).getRank() == cards.get(1).getRank() &&
               cards.get(1).getRank() == cards.get(2).getRank() &&
               cards.get(2).getRank() == cards.get(3).getRank();
    }

    private static boolean isStraight(List<Card> cards) {
        // 2不能参与顺子
        for (Card card : cards) {
            if (card.getRank() == RANK_2) return false;
        }
        for (int i = 0; i < cards.size() - 1; i++) {
            int rank1 = cards.get(i).getRank();
            int rank2 = cards.get(i + 1).getRank();
            if (rank1 != rank2 + 1) return false;
        }
        return true;
    }

    private static boolean isConsecutivePairs(List<Card> cards) {
        // 2不能参与连对
        for (Card card : cards) {
            if (card.getRank() == RANK_2) return false;
        }
        for (int i = 0; i < cards.size(); i += 2) {
            if (cards.get(i).getRank() != cards.get(i + 1).getRank()) return false;
            if (i > 0) {
                int rank1 = cards.get(i - 2).getRank();
                int rank2 = cards.get(i).getRank();
                if (rank1 != rank2 + 1) return false;
            }
        }
        return true;
    }

    /**
     * 比较两手牌大小
     * @param prev 上一手牌
     * @param current 当前手牌
     * @return true if current > prev
     */
    public static boolean compare(List<Card> prev, List<Card> current) {
        ToSouthCardType type1 = getCardType(prev);
        ToSouthCardType type2 = getCardType(current);

        if (type2 == ToSouthCardType.NONE) return false;
        // 炸弹牌型，只能炸2的牌型和三连对，不能炸顺子、单张对子3-A等普通牌型
        if (type2 == ToSouthCardType.BOMB_QUAD) {
            if (type1 == ToSouthCardType.BOMB_QUAD) {
                // 都是四张，比大小
                return compareMaxCard(prev, current);
            }
            // 规则2: 炸弹不能通杀，只能炸2的牌型和三连对牌型
            // 2-1: 炸单张2
            if (type1 == ToSouthCardType.SINGLE && prev.getFirst().getRank() == RANK_2) return true;
            // 2-2: 炸对子2
            if (type1 == ToSouthCardType.PAIR && prev.getFirst().getRank() == RANK_2) return true;
            // 2-3: 炸三连对 其他牌型 (如顺子、单张/对子3-A、三张等) 不能炸
            if (type1 == ToSouthCardType.CONSECUTIVE_PAIRS) return prev.size() < 8;
            return false;
        }

        // 连对: 只能大过 单2/对2/同类型
        if (type2 == ToSouthCardType.CONSECUTIVE_PAIRS) {
            // 3连对(6张) > 单张2
            if (current.size() == 6 && type1 == ToSouthCardType.SINGLE && prev.getFirst().getRank() == RANK_2) return true;
            // 四连对可以炸掉两张2、一张2、四张和同类型牌型，不能大过其他牌型；
            if (current.size() == 8) {
                if ((type1 == ToSouthCardType.PAIR || type1 == ToSouthCardType.SINGLE) && prev.getFirst().getRank() == RANK_2) return true;
                if (type1 == ToSouthCardType.BOMB_QUAD) return true;
            }

            // 连对 vs 连对
            if (type1 == ToSouthCardType.CONSECUTIVE_PAIRS && prev.size() == current.size()) {
                return compareMaxCard(prev, current);
            }
            
            // 连对不能大过其他牌型(如三张、顺子等)
            return false;
        }
        // 普通牌型必须类型相同且张数相同
        if (type1 != type2 || prev.size() != current.size()) return false;
        // 比较最大牌
        return compareMaxCard(prev, current);
    }

    private static boolean isRedPig(Card card) {
        return card != null && card.getRank() == RANK_2 && card.getSuit() == HEART_SUIT;
    }

    private static boolean isBombType(ToSouthCardType type) {
        return type == ToSouthCardType.BOMB_QUAD || type == ToSouthCardType.CONSECUTIVE_PAIRS;
    }


    private static boolean compareMaxCard(List<Card> prev, List<Card> current) {
        // 均已降序排序，取第一张（最大张）比较
        return CARD_COMPARATOR.compare(current.getFirst(), prev.getFirst()) < 0;
    }

    /**
     * 获取通杀牌的 ID 列表
     */
    public static Pair<Integer, List<Integer>> getInstantWinCards(List<Card> cards) {
        List<Integer> cardClientIds = new ArrayList<>();
        if (cards == null || cards.size() != 13) return null;
        cards.sort(CARD_COMPARATOR);

        // 4个2
        if (countRank(cards, RANK_2) == 4) {
            for (Card c : cards) {
                if (c.getRank() == RANK_2 && c instanceof PokerCard pc) {
                    cardClientIds.add(pc.getClientId());
                }
            }
            return new Pair<>(FOUR_TWO, cardClientIds);
        }
        // 一条龙 (3-A)
        if (isDragon(cards)) {
            // 所有非2的牌都是通杀牌
            for (Card c : cards) {
                if (c.getRank() != RANK_2 && c instanceof PokerCard pc) {
                    cardClientIds.add(pc.getClientId());
                }
            }
            return new Pair<>(DRAGON, cardClientIds);
        }
        // 同色
        if (isAllSameColor(cards)) {
            // 全都是通杀牌
            for (Card c : cards) {
                if (c instanceof PokerCard pc) cardClientIds.add(pc.getClientId());
            }
            return new Pair<>(SAME_COLOR, cardClientIds);
        }
        // 6对
        if (countPairs(cards) >= 6) {
            // 找出所有对子
            List<Integer> pairRanks = new ArrayList<>();
            for (int i = 0; i < cards.size() - 1; i++) {
                if (cards.get(i).getRank() == cards.get(i+1).getRank()) {
                    pairRanks.add(cards.get(i).getRank());
                    i++;
                }
            }
            // 复用 countPairs 逻辑
            int pairsFound = 0;
            for (int i = 0; i < cards.size() - 1; i++) {
                if (cards.get(i).getRank() == cards.get(i+1).getRank()) {
                    if (cards.get(i) instanceof PokerCard pc1) cardClientIds.add(pc1.getClientId());
                    if (cards.get(i+1) instanceof PokerCard pc2) cardClientIds.add(pc2.getClientId());
                    pairsFound++;
                    i++; 
                    if (pairsFound == 6) break;
                }
            }
            return new Pair<>(SIX_PAIRS, cardClientIds);
        }
        // 5连对、6连对
        Map<Integer, List<Card>> rankMap = convertCardListToRankMap(cards);
        List<Integer> pairs = new ArrayList<>();
        for (Map.Entry<Integer, List<Card>> entry : rankMap.entrySet()) {
            if (entry.getValue().size() >= 2) {
                pairs.add(entry.getKey());
            }
        }
        pairs.removeIf(r -> r == RANK_2);
        pairs.sort(Comparator.reverseOrder());
        List<Integer> consec = findConsecutiveSubList(pairs, 3);
        if (consec != null && consec.size() >= 5) {
            for (Integer r : consec) {
                List<Card> cs = rankMap.get(r);
                for (int k=0; k<2; k++) {
                    if (cs.get(k) instanceof PokerCard pc) cardClientIds.add(pc.getClientId());
                }
            }
            return new Pair<>(consec.size() > 5 ? SIX_CONSEC_PAIRS : FIVE_CONSEC_PAIRS, cardClientIds);
        }
        return null;
    }

    public static int countTwo(List<Card> cards) {
        return countRank(cards, RANK_2);
    }

    public static int countBomb(List<Card> cards) {
        cards.sort(CARD_COMPARATOR);
        return countQuads(cards);
    }

    private static int countRank(List<Card> cards, int rank) {
        int count = 0;
        for (Card c : cards) {
            if (c.getRank() == rank) count++;
        }
        return count;
    }

    private static boolean isDragon(List<Card> cards) {
        Set<Integer> ranks = new HashSet<>();
        for (Card c : cards) {
            // 2排除在一条龙之外
            if (c.getRank() == RANK_2) {
                continue;
            }
            ranks.add(c.getRank());
        }
        return ranks.size() == 12;
    }

    private static int countPairs(List<Card> cards) {
        int pairs = 0;
        int i = 0;
        while (i < cards.size() - 1) {
            if (cards.get(i).getRank() == cards.get(i + 1).getRank()) {
                // 四张同点是炸弹，不计入对子，整组跳过
                if (i + 3 < cards.size() && cards.get(i).getRank() == cards.get(i + 3).getRank()) {
                    i += 4;
                } else {
                    pairs++;
                    i += 2;
                }
            } else {
                i++;
            }
        }
        return pairs;
    }

    private static int countQuads(List<Card> cards) {
        int quads = 0;
        for (int i = 0; i < cards.size() - 3; i++) {
            if (cards.get(i).getRank() == cards.get(i+3).getRank()) {
                quads++;
                i+=3;
            }
        }
        return quads;
    }

    private static boolean isAllSameColor(List<Card> cards) {
        // 同花色，黑桃/梅花混合 或 红桃/方块混合 (即同为黑色或红色)
        boolean isRed = isRed(cards.getFirst());
        for (Card c : cards) {
            if (isRed(c) != isRed) return false;
        }
        return true;
    }

    private static boolean isRed(Card c) {
        return c.getSuit() == HEART_SUIT || c.getSuit() == DIAMOND_SUIT; // 1:♥, 2:♦
    }

    public static Map<Integer, List<Card>> convertCardListToRankMap(List<Card> cards) {
        Map<Integer, List<Card>> rankMap = new HashMap<>();
        for (Card c : cards) {
            rankMap.computeIfAbsent(c.getRank(), k -> new ArrayList<>()).add(c);
        }
        return rankMap;
    }

    /**
     * 找到从指定牌开始最佳组合，用于机器人和托管
     *  // 优先级：炸弹 > 三张 > 连对 > 顺子 > 对子 > 单张
     * @param rankMap 以 rank 为 key 的手牌
     * @param firstCard 最小那张牌
     * @return 组合
     */
    public static List<Card> findBestPlayWithFirstCard(Map<Integer, List<Card>> rankMap, Card firstCard) {
        List<Card> firstRankCards = rankMap.get(firstCard.getRank());
        if (CollUtil.isEmpty(firstRankCards)) return null;
        if (firstRankCards.stream().noneMatch(card -> card.getValue() == firstCard.getValue())) {
            return null;
        }
        int rank = firstCard.getRank();
        // 1. 炸弹
        if (rankMap.get(rank).size() == 4) {
            return rankMap.get(rank);
        }
        // 2. 三张
        if (rankMap.get(rank).size() == 3) {
            return rankMap.get(rank);
        }
        // 3. 连对或者顺子
        if (rankMap.containsKey(rank)) {
            List<Card> consecutiveCards = findConsecutiveCards(rankMap, firstCard);
            if (consecutiveCards != null) {
                return consecutiveCards;
            }
        }
        // 4. 对子
        if (rankMap.get(rank).size() == 2) {
            return rankMap.get(rank);
        }
        // 5. 单张
        if (rankMap.get(rank).size() == 1) {
            return rankMap.get(rank);
        }
        
        // 兜底：如果 rankMap.get(rank) 还是 > 0 (比如顺子没凑成，且数量>2)，直接返回第一张
        if (!rankMap.get(rank).isEmpty()) {
            return List.of(firstCard);
        }

        return null;
    }

    /**
     * 查找所有可能的最佳出牌组合（首出）
     * 策略：
     * 1. 首先将手牌按照 炸弹 > 三张 > 连对 > 顺子 > 对子 > 单张 的优先级进行整合拆分。
     * 2. 然后按照以下顺序查找所有可出牌型：
     *    单张 > 对子 > 三条 > 顺子 > 连对 > 炸弹
     * @param handCards 手牌列表
     * @return 所有可出的牌型列表
     */
    public static List<List<Card>> findAllBestPlays(List<Card> handCards) {
        if (CollUtil.isEmpty(handCards)) return Collections.emptyList();

        List<List<Card>> result = new ArrayList<>();
        
        // 1. 整合牌型 (分解手牌)
        Map<ToSouthCardType, List<List<Card>>> integratedCards = integrateHandCards(handCards);

        // 2. 按优先级查找
        // 优先级：顺子 > 三条 > 对子 > 单张 > 连对 > 炸弹

        // 2.4 顺子
        List<List<Card>> straights = integratedCards.get(ToSouthCardType.STRAIGHT);
        if (CollUtil.isNotEmpty(straights)) result.addAll(straights);

        // 2.3 三张
        List<List<Card>> triples = integratedCards.get(ToSouthCardType.TRIPLE);
        if (CollUtil.isNotEmpty(triples)) result.addAll(triples);

        // 2.2 对子
        List<List<Card>> pairs = integratedCards.get(ToSouthCardType.PAIR);
        if (CollUtil.isNotEmpty(pairs)) result.addAll(pairs);

        // 2.1 单张
        List<List<Card>> singles = integratedCards.get(ToSouthCardType.SINGLE);
        if (CollUtil.isNotEmpty(singles)) result.addAll(singles);

        // 2.5 连对
        List<List<Card>> consecutivePairs = integratedCards.get(ToSouthCardType.CONSECUTIVE_PAIRS);
        if (CollUtil.isNotEmpty(consecutivePairs)) result.addAll(consecutivePairs);

        // 2.6 炸弹
        List<List<Card>> bombs = integratedCards.get(ToSouthCardType.BOMB_QUAD);
        if (CollUtil.isNotEmpty(bombs)) result.addAll(bombs);

        // 兜底：如果没识别出任何牌型（理论上不会，至少有单张），直接出最小的一张
        if (result.isEmpty()) {
            handCards.sort(CARD_COMPARATOR);

            result.add(List.of(handCards.getLast()));
        }
        
        // 对结果去重并排序 (从小到大)
        result.sort((list1, list2) -> {
            // 先按牌型类型排序 (单张 < 对子 < 三张 < 顺子 < 连对 < 炸弹)
             ToSouthCardType t1 = getCardType(list1);
             ToSouthCardType t2 = getCardType(list2);
             // 如果类型不同，按枚举顺序
             if (t1 != t2) return Integer.compare(getCardTypePriority(t1), getCardTypePriority(t2));
             
             // 如果类型相同，按最大牌比较
            Card max1 = list1.getFirst();
            Card max2 = list2.getFirst();
            return CARD_COMPARATOR.compare(max2, max1); // 升序 (小在前)
        });

        return result;
    }

    private static int getCardTypePriority(ToSouthCardType type) {
        return switch (type) {
            case SINGLE -> 4;
            case PAIR -> 3;
            case TRIPLE -> 2;
            case STRAIGHT -> 1;
            case CONSECUTIVE_PAIRS -> 5;
            case BOMB_QUAD -> 6;
            default -> 0;
        };
    }

    /**
     * 查找所有大于目标牌型的可出手牌组合
     *
     * @param handCards 手牌列表
     * @param lastCards 上家出的牌
     * @return 所有可压制的牌型列表
     */
    public static List<List<Card>> findAllFollowPlays(List<Card> handCards, List<Card> lastCards) {
        if (CollUtil.isEmpty(handCards) || CollUtil.isEmpty(lastCards)) return Collections.emptyList();

        List<List<Card>> result = new ArrayList<>();
        
        // 识别上家牌型
        ToSouthCardType lastType = getCardType(lastCards);
        if (lastType == ToSouthCardType.NONE) return Collections.emptyList();

        // 1. 尝试找同类型压制
        Map<ToSouthCardType, List<List<Card>>> integratedCards = integrateHandCards(handCards);

        if (lastType == ToSouthCardType.SINGLE) {
            // 单张跟牌：手牌中任意一张比上家大的单张都可以出。
            // 不能依赖 integrateHandCards 的结果——对子/三张/炸弹中的牌不会进入 SINGLE 分组，
            // 导致"有对3却被判为无法跟单3"的错误。直接遍历全部手牌。
            for (Card card : handCards) {
                List<Card> singleCard = new ArrayList<>();
                singleCard.add(card);
                if (compare(lastCards, singleCard)) {
                    result.add(singleCard);
                }
            }
        } else if (lastType == ToSouthCardType.PAIR) {
            // 对子跟牌：手牌中任意一对比上家大的对子都可以出（包括从三张、炸弹中拆出对子）。
            // integrateHandCards 会先提走三张/炸弹，导致 PAIR 分组中漏掉可拆出的对子。
            Map<Integer, List<Card>> allRankMap = new HashMap<>();
            for (Card card : handCards) {
                allRankMap.computeIfAbsent(card.getRank(), k -> new ArrayList<>()).add(card);
            }
            for (Map.Entry<Integer, List<Card>> entry : allRankMap.entrySet()) {
                List<Card> rankCards = new ArrayList<>(entry.getValue());
                if (rankCards.size() >= 2) {
                    rankCards.sort(CARD_COMPARATOR); // 降序：最大花色排最前
                    // 取最强两张作为推荐对子
                    List<Card> bestPair = new ArrayList<>();
                    bestPair.add(rankCards.get(0));
                    bestPair.add(rankCards.get(1));
                    if (compare(lastCards, bestPair)) {
                        result.add(bestPair);
                    }
                }
            }
        } else if (lastType == ToSouthCardType.TRIPLE) {
            // 三张跟牌：手牌中任意一组三张比上家大的都可以出（包括从炸弹中拆出三张）。
            Map<Integer, List<Card>> allRankMap = new HashMap<>();
            for (Card card : handCards) {
                allRankMap.computeIfAbsent(card.getRank(), k -> new ArrayList<>()).add(card);
            }
            for (Map.Entry<Integer, List<Card>> entry : allRankMap.entrySet()) {
                List<Card> rankCards = new ArrayList<>(entry.getValue());
                if (rankCards.size() >= 3) {
                    rankCards.sort(CARD_COMPARATOR); // 降序
                    List<Card> bestTriple = new ArrayList<>();
                    bestTriple.add(rankCards.get(0));
                    bestTriple.add(rankCards.get(1));
                    bestTriple.add(rankCards.get(2));
                    if (compare(lastCards, bestTriple)) {
                        result.add(bestTriple);
                    }
                }
            }
        } else {
            // 顺子、连对等：使用 integratedCards 的分组结果（这些牌型本身就需要整体出，不存在拆分问题）
            List<List<Card>> sameTypeCandidates = integratedCards.get(lastType);

            if (CollUtil.isNotEmpty(sameTypeCandidates)) {
                // sameTypeCandidates 是降序排列的 (大 -> 小)
                for (List<Card> candidate : sameTypeCandidates) {
                    // 如果找到的 candidate 比 lastCards 长，需要截取
                    if (lastType == ToSouthCardType.STRAIGHT || lastType == ToSouthCardType.CONSECUTIVE_PAIRS) {
                        if (candidate.size() > lastCards.size()) {
                            // 尝试截取同等长度
                            List<Card> sortedCandidate = new ArrayList<>(candidate);
                            sortedCandidate.sort((c1, c2) -> Integer.compare(c1.getRank(), c2.getRank()));

                            int step = (lastType == ToSouthCardType.STRAIGHT) ? 1 : 2;
                            int targetSize = lastCards.size();

                            // 滑动窗口查找所有能压过的子串
                            // 注意：必须用 new ArrayList<> 拷贝，不能用 subList 视图。
                            // compare -> getCardType 会对入参 in-place 排序，若传视图会破坏
                            // sortedCandidate 后续位置的顺序，导致窗口滑到脏数据。
                            for (int j = 0; j <= sortedCandidate.size() - targetSize; j += step) {
                                List<Card> subList = new ArrayList<>(sortedCandidate.subList(j, j + targetSize));
                                if (compare(lastCards, subList)) {
                                    result.add(subList);
                                }
                            }
                            continue;
                        }
                    }

                    if (compare(lastCards, candidate)) {
                        result.add(candidate);
                    }
                }
            }
        }
        
        // 2. 尝试用连对压制 2 (特殊规则)
        List<List<Card>> consecutivePairs = integratedCards.get(ToSouthCardType.CONSECUTIVE_PAIRS);
        if (CollUtil.isNotEmpty(consecutivePairs)) {
             if (lastType == ToSouthCardType.SINGLE && lastCards.getFirst().getRank() == RANK_2) {
                 // 上家是单2，优先找4连对(8张)，没有则找3连对(6张)
                 for (List<Card> cp : consecutivePairs) {
                     // 排序，确保从小到大
                     List<Card> sortedCp = new ArrayList<>(cp);
                     sortedCp.sort((c1, c2) -> Integer.compare(c1.getRank(), c2.getRank()));

                     if (sortedCp.size() >= 8) {
                         // 优先出4连对 (截取前8张)
                         result.add(sortedCp.subList(0, 8));
                     } else if (sortedCp.size() >= 6) {
                         // 其次出3连对 (截取前6张)
                         result.add(sortedCp.subList(0, 6));
                     }
                 }
             } else if (lastType == ToSouthCardType.PAIR && lastCards.getFirst().getRank() == RANK_2) {
                 // 上家是对2，找4连对(8张)
                 for (List<Card> cp : consecutivePairs) {
                     if (cp.size() >= 8) {
                         List<Card> sortedCp = new ArrayList<>(cp);
                         sortedCp.sort((c1, c2) -> Integer.compare(c1.getRank(), c2.getRank()));
                         // 截取前8张
                        result.add(sortedCp.subList(0, 8));
                    }
                }
            } else if (lastType == ToSouthCardType.BOMB_QUAD) {
                for (List<Card> cp : consecutivePairs) {
                    if (cp.size() >= 8) {
                        List<Card> sortedCp = new ArrayList<>(cp);
                        sortedCp.sort((c1, c2) -> Integer.compare(c1.getRank(), c2.getRank()));
                        List<Card> candidate = new ArrayList<>(sortedCp.subList(0, 8));
                        if (compare(lastCards, candidate)) {
                            result.add(candidate);
                        }
                    }
                }
            }
        }
        
        // 3. 尝试用炸弹压制
        List<List<Card>> bombs = integratedCards.get(ToSouthCardType.BOMB_QUAD);
        if (CollUtil.isNotEmpty(bombs)) {
             // 遍历所有炸弹，检查是否能压制
             for (List<Card> bomb : bombs) {
                 if (compare(lastCards, bomb)) {
                     result.add(bomb);
                 }
             }
        }
        
        // 对结果去重并排序 (从小到大)
        result.sort((list1, list2) -> {
            // 先按牌型类型排序 (普通 < 连对压2 < 炸弹)
             ToSouthCardType t1 = getCardType(list1);
             ToSouthCardType t2 = getCardType(list2);
             // 如果类型不同，炸弹排最后
             boolean isBomb1 = isBombType(t1);
             boolean isBomb2 = isBombType(t2);
             if (isBomb1 != isBomb2) return isBomb1 ? 1 : -1;
             
             // 如果类型相同，按最大牌比较
            Card max1 = list1.getFirst();
            Card max2 = list2.getFirst();
            return CARD_COMPARATOR.compare(max2, max1); // 升序 (小在前)
        });

        return result;
    }

    /**
     * 查找最佳首出牌型（最小）
     * @param handCards 手牌
     * @return 最佳牌型，若无则返回null
     */
    public static List<Card> findBestPlay(List<Card> handCards) {
        List<List<Card>> allPlays = findAllBestPlays(handCards);
        return CollUtil.isNotEmpty(allPlays) ? allPlays.getFirst() : null;
    }

    /**
     * 查找大于目标牌型的最小手牌组合
     *
     * @param handCards 手牌列表
     * @param lastCards 上家出的牌
     * @return 可压制的最小牌型，如果没有则返回 null
     */
    public static List<Card> findBestFollowPlay(List<Card> handCards, List<Card> lastCards) {
        List<List<Card>> allPlays = findAllFollowPlays(handCards, lastCards);
        return CollUtil.isNotEmpty(allPlays) ? allPlays.getFirst() : null;
    }

    /**
     * 从组合列表中获取最小的一组
     */
    private static List<Card> getSmallestCombination(List<List<Card>> combinations) {
        combinations.sort((list1, list2) -> {
            Card max1 = list1.getFirst();
            Card max2 = list2.getFirst();
            return CARD_COMPARATOR.compare(max2, max1); 
        });
        // 排序后是 [Min ... Max]
        // 取第一个 (最小的)
        return combinations.getFirst();
    }

    /**
     * 对手牌进行排序，并返回需要高亮的牌ID（2、炸弹、连对）
     * 排序规则：炸弹 > 连对 > 顺子 > 三条 > 对子 > 单张
     * @param cards 手牌列表 (会被修改为排序后的顺序)
     * @return 高亮牌 ID 列表
     */
    public static List<Integer> sortAndGetHighlightCards(List<Card> cards) {
        List<Integer> highlightIds = new ArrayList<>();
        if (cards == null || cards.isEmpty()) return highlightIds;

        Map<ToSouthCardType, List<List<Card>>> integrated = integrateHandCards(cards);

        List<List<Card>> bombs = integrated.get(ToSouthCardType.BOMB_QUAD);
        if (CollUtil.isNotEmpty(bombs)) {
            for (List<Card> bomb : bombs) {
                for (Card c : bomb) {
                    if (c instanceof PokerCard pc) highlightIds.add(pc.getClientId());
                }
            }
        }
        List<List<Card>> cps = integrated.get(ToSouthCardType.CONSECUTIVE_PAIRS);
        if (CollUtil.isNotEmpty(cps)) {
            for (List<Card> cp : cps) {
                for (Card c : cp) {
                    if (c instanceof PokerCard pc) highlightIds.add(pc.getClientId());
                }
            }
        }
        for (Card c : cards) {
            if (c.getRank() == RANK_2) {
                if (c instanceof PokerCard pc) {
                    if (!highlightIds.contains(pc.getClientId())) {
                        highlightIds.add(pc.getClientId());
                    }
                }
            }
        }

        List<Card> sorted = new ArrayList<>();

        addGroups(sorted, integrated.get(ToSouthCardType.BOMB_QUAD));
        addGroups(sorted, integrated.get(ToSouthCardType.CONSECUTIVE_PAIRS));
        addGroups(sorted, integrated.get(ToSouthCardType.STRAIGHT));
        addGroups(sorted, integrated.get(ToSouthCardType.TRIPLE));
        addGroups(sorted, integrated.get(ToSouthCardType.PAIR));
        addGroups(sorted, integrated.get(ToSouthCardType.SINGLE));

        cards.clear();
        cards.addAll(sorted);

        return highlightIds;
    }

    private static void addGroups(List<Card> target, List<List<Card>> groups) {
        if (groups == null || groups.isEmpty()) return;
        groups.sort((g1, g2) -> CARD_COMPARATOR.compare(g1.getFirst(), g2.getFirst())); // CARD_COMPARATOR is desc
        for (List<Card> group : groups) {
            group.sort(CARD_COMPARATOR);
            target.addAll(group);
        }
    }

    /**
     * 整合手牌：将散乱的手牌按规则整理成具体的牌型集合
     * 整理顺序：炸弹 > 三张 > 连对 > 顺子 > 对子 > 单张
     */
    public static Map<ToSouthCardType, List<List<Card>>> integrateHandCards(List<Card> handCards) {
        Map<ToSouthCardType, List<List<Card>>> result = new HashMap<>();
        // 复制并排序 (降序 2..3)
        List<Card> cards = new ArrayList<>(handCards);
        cards.sort(CARD_COMPARATOR);
        
        Map<Integer, List<Card>> rankMap = new TreeMap<>((r1, r2) -> Integer.compare(r2, r1)); // key 降序
        for (Card c : cards) {
            rankMap.computeIfAbsent(c.getRank(), k -> new ArrayList<>()).add(c);
        }

        // 1. 提取炸弹 (4张)
        List<List<Card>> bombs = new ArrayList<>();
        Iterator<Map.Entry<Integer, List<Card>>> it = rankMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<Card>> entry = it.next();
            if (entry.getValue().size() == 4) {
                bombs.add(entry.getValue());
                it.remove();
            }
        }
        result.put(ToSouthCardType.BOMB_QUAD, bombs);

        // 2. 提取三张
        // 注意：不拆分3张去凑连对或顺子（遵循不拆 >2 的原则）
        List<List<Card>> triples = new ArrayList<>();
        it = rankMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<Card>> entry = it.next();
            if (entry.getValue().size() == 3) {
                triples.add(entry.getValue());
                it.remove();
            }
        }
        result.put(ToSouthCardType.TRIPLE, triples);

        // 3. 提取连对 (从剩余的对子中找)
        List<List<Card>> consecutivePairs = new ArrayList<>();
        // 获取所有 count == 2 的 rank
        List<Integer> pairRanks = new ArrayList<>();
        for (Map.Entry<Integer, List<Card>> entry : rankMap.entrySet()) {
            if (entry.getValue().size() == 2) {
                pairRanks.add(entry.getKey());
            }
        }
        // 2不参与连对
        pairRanks.removeIf(r -> r == RANK_2);
        // pairRanks 是降序的 (e.g. A, K, Q...)
        // 找连续序列 (逻辑值连续)
        if (pairRanks.size() >= 3) {
            // 简单的贪心查找
            // 比如 6, 5, 4, 3 -> 6-3 连对
            List<Integer> currentChain = new ArrayList<>();
            for (int r : pairRanks) {
                if (currentChain.isEmpty()) {
                    currentChain.add(r);
                } else {
                    int lastR = currentChain.getLast();
                    if (lastR == r + 1) {
                        currentChain.add(r);
                    } else {
                        // 链断了
                        if (currentChain.size() >= 3) {
                            addConsecutivePairs(consecutivePairs, rankMap, currentChain);
                        }
                        currentChain.clear();
                        currentChain.add(r);
                    }
                }
            }
            if (currentChain.size() >= 3) {
                addConsecutivePairs(consecutivePairs, rankMap, currentChain);
            }
        }
        result.put(ToSouthCardType.CONSECUTIVE_PAIRS, consecutivePairs);

        // 策略：将所有剩余牌视为单张池，找最长顺子
        List<List<Card>> straights = new ArrayList<>();
        // 获取所有存在的 rank
        List<Integer> singleRanks = new ArrayList<>();
        for (Map.Entry<Integer, List<Card>> entry : rankMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                singleRanks.add(entry.getKey());
            }
        }
        // 降序
        singleRanks.sort((r1, r2) -> Integer.compare(r2, r1));
        // 2不参与顺子
        singleRanks.removeIf(r -> r == RANK_2);
        
        if (singleRanks.size() >= 3) { // 顺子至少3张
             List<Integer> currentChain = new ArrayList<>();
            for (int r : singleRanks) {
                if (currentChain.isEmpty()) {
                    currentChain.add(r);
                } else {
                    int lastR = currentChain.getLast();
                    if (lastR == r + 1) {
                        currentChain.add(r);
                    } else {
                        if (currentChain.size() >= 3) {
                            addStraight(straights, rankMap, currentChain);
                        }
                        currentChain.clear();
                        currentChain.add(r);
                    }
                }
            }
             if (currentChain.size() >= 3) {
                 addStraight(straights, rankMap, currentChain);
             }
        }
        result.put(ToSouthCardType.STRAIGHT, straights);

        // 5. 剩余的归为对子或单张
        List<List<Card>> pairs = new ArrayList<>();
        List<List<Card>> singles = new ArrayList<>();
        
        for (Map.Entry<Integer, List<Card>> entry : rankMap.entrySet()) {
            List<Card> cs = entry.getValue();
            if (cs.isEmpty()) continue;
            // 顺子可能会消耗掉牌，这里需要重新检查数量
            if (cs.size() == 2) {
                pairs.add(cs);
            } else if (cs.size() == 1) {
                singles.add(cs);
            } else {
                while (cs.size() >= 2) {
                     List<Card> pair = new ArrayList<>();
                     pair.add(cs.removeFirst());
                     pair.add(cs.removeFirst());
                     pairs.add(pair);
                }
                if (!cs.isEmpty()) {
                    singles.add(cs);
                }
            }
        }
        result.put(ToSouthCardType.PAIR, pairs);
        result.put(ToSouthCardType.SINGLE, singles);

        return result;
    }

    private static void addConsecutivePairs(List<List<Card>> target, Map<Integer, List<Card>> rankMap, List<Integer> ranks) {
        if (ranks.isEmpty()) return;
        List<Card> chain = new ArrayList<>();
        
        // 规则：连对最大的对子取最强的花色，其他的对子取最弱的花色
        // ranks 是降序排列 (e.g. 5, 4, 3)
        // 所以 ranks.get(0) 是最大对子
        int maxRank = ranks.get(0);

        for (Integer r : ranks) {
            List<Card> cs = rankMap.get(r);
            if (cs != null && !cs.isEmpty()) {
                cs.sort(CARD_COMPARATOR); 
                
                List<Card> selected = new ArrayList<>();
                if (r == maxRank) {
                    // 最大对子取最强花色 (取最后两张)
                    if (cs.size() >= 2) {
                        selected.add(cs.removeLast());
                        selected.add(cs.removeLast());
                    }
                } else {
                    // 其他对子取最弱花色 (取前两张)
                    if (cs.size() >= 2) {
                        selected.add(cs.removeFirst());
                        selected.add(cs.removeFirst());
                    }
                }
                chain.addAll(selected);
            }
        }
        target.add(chain);
    }

    private static void addStraight(List<List<Card>> target, Map<Integer, List<Card>> rankMap, List<Integer> ranks) {
        if (ranks.isEmpty()) return;
        List<Card> chain = new ArrayList<>();
        
        // 规则：顺子最大的那张牌若有多张，取花色最强的那张；顺子其他牌都取花色最弱的
        // ranks 是降序排列 (e.g. 7, 6, 5, 4, 3)
        // ranks.get(0) 是最大牌
        int maxRank = ranks.get(0);

        for (Integer r : ranks) {
            List<Card> cs = rankMap.get(r);
            if (cs != null && !cs.isEmpty()) {
                cs.sort(CARD_COMPARATOR);
                
                if (r == maxRank) {
                    // 最大牌取最强花色 (取最后一张)
                    chain.add(cs.removeLast());
                } else {
                    // 其他牌取最弱花色 (取第一张)
                    chain.add(cs.removeFirst());
                }
            }
        }
        target.add(chain);
    }

    /**
     * 获取从指定牌开始的顺子（单牌、对子），要求顺子中不拆牌
     * 比如34456789，指定从3开始，由于4是多个，则获取不到顺子
     * @param rankMap 以rank为key的map
     * @return 成顺的牌或 null
     */
    public static List<Card> findConsecutiveCards(Map<Integer, List<Card>> rankMap, Card firstCard) {
        // 2不能参与顺子
        if (firstCard.getRank() == RANK_2) {
            return null;
        }

        List<Card> firstRankCards = rankMap.get(firstCard.getRank());
        if (CollUtil.isEmpty(firstRankCards)) return null;
        if (firstRankCards.stream().noneMatch(card -> card.getValue() == firstCard.getValue())) {
            return null;
        }
        // 只能组成单牌顺子和顺对
        int firstRankCount = firstRankCards.size();
        if (firstRankCount != 1 && firstRankCount != 2) {
            return null;
        }
        List<Card> currentChain = new ArrayList<>(firstRankCards);

        int currentRank = firstCard.getRank();
        while (true) {
            int nextRank;
            if (currentRank == 14) break; // A -> End
            else nextRank = currentRank + 1;
            
            List<Card> nextCards = rankMap.get(nextRank);
            if (nextCards == null || nextCards.size() != firstRankCount) {
                break;
            }
            currentChain.addAll(nextCards);
            currentRank = nextRank;
        }
        if ((firstRankCount == 1 && currentChain.size() >= 3) || (firstRankCount == 2 && currentChain.size() >= 6)) {
            return currentChain;
        }
        return null;
    }

    private static List<Integer> findConsecutiveSubList(List<Integer> sortedRanks, int minLen) {
        if (sortedRanks.size() < minLen) return null;
        List<Integer> currentChain = new ArrayList<>();
        for (Integer r : sortedRanks) {
            if (currentChain.isEmpty()) {
                currentChain.add(r);
            } else {
                if (currentChain.getLast() == r + 1) {
                    currentChain.add(r);
                } else {
                    if (currentChain.size() >= minLen) return currentChain;
                    currentChain.clear();
                    currentChain.add(r);
                }
            }
        }
        if (currentChain.size() >= minLen) return currentChain;
        return null;
    }

    public static void main(String[] args) {
        System.out.println("=== 南方前进牌型工具类测试 ===");
        
        // 1. 基础随机手牌测试 (5组)
        System.out.println("--- 基础手牌整合与最佳出牌测试 ---");
        for (int i = 0; i < 5; i++) {
            System.out.println("\n测试用例 " + (i + 1) + ":");
            List<Card> hand = generateRandomHand(13);
            System.out.println("手牌: " + handToString(hand));

            Map<ToSouthCardType, List<List<Card>>> integrated = integrateHandCards(hand);
            System.out.println("牌型结构:");
            integrated.forEach((k, v) -> {
                if (CollUtil.isNotEmpty(v)) {
                    System.out.print(k + ": ");
                    for (List<Card> c : v) {
                        System.out.print(handToString(c) + " ");
                    }
                    System.out.println();
                }
            });

            long start = System.currentTimeMillis();
            List<Card> bestPlay = findBestPlay(new ArrayList<>(hand));
            long end = System.currentTimeMillis();

            System.out.println("最佳出牌: " + handToString(bestPlay));
            System.out.println("耗时: " + (end - start) + "ms");
        }

//        // 2. 随机压牌测试 (10组)
//        System.out.println("\n--- 随机压牌测试 (模拟跟牌) ---");
//        for (int i = 0; i < 10; i++) {
//            System.out.println("\n测试用例 " + (i + 1) + ":");
//            // 随机生成两手牌，一手作为手牌(13张)，一手作为上家出的牌(随机1-5张)
//            List<Card> myHand = generateRandomHand(13);
//
//            // 随机生成上家牌型 (这里简单模拟，随机取几张牌)
//            // 为了更有意义，我们尝试生成一个合法的上家牌型
//            // 先生成一副完整手牌，然后从中提取一个最佳出牌作为上家牌
//            List<Card> opponentHand = generateRandomHand(13);
//            List<Card> lastPlay = findBestPlay(opponentHand);
//
//            if (CollUtil.isEmpty(lastPlay)) {
//                System.out.println("上家无牌可出(异常)，跳过");
//                continue;
//            }
//
//            System.out.println("我方手牌: " + handToString(myHand));
//            System.out.println("上家出牌: " + handToString(lastPlay));
//
//            long s = System.currentTimeMillis();
//            List<Card> follow = findBestFollowPlay(myHand, lastPlay);
//            long e = System.currentTimeMillis();
//
//            if (follow != null) {
//                System.out.println("我方压制: " + handToString(follow));
//            } else {
//                System.out.println("我方要不起 (Pass)");
//            }
//            System.out.println("耗时: " + (e - s) + "ms");
//        }
    }

    private static List<Card> generateRandomHand(int count) {
        List<Card> deck = new ArrayList<>();
//        for (int s = 1; s <= 4; s++) {
//            for (int r = 3; r <= 15; r++) {
//                deck.add(new Card(s, r));
//            }
//        }
        deck.add(new Card(2,15));
        deck.add(new Card(4,15));
        deck.add(new Card(2,14));
        deck.add(new Card(4,13));
        deck.add(new Card(2,11));
        deck.add(new Card(1,9));
        deck.add(new Card(3,9));
        deck.add(new Card(2,8));
        deck.add(new Card(4,8));
        deck.add(new Card(2,7));
        deck.add(new Card(4,7));
        deck.add(new Card(2,6));
        deck.add(new Card(3,3));

//        Collections.shuffle(deck);
        return new ArrayList<>(deck.subList(0, count));
    }

    private static String handToString(List<Card> cards) {
        if (cards == null) return "null";
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(CARD_COMPARATOR);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(cardToString(sorted.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String cardToString(Card c) {
        String suitStr = switch (c.getSuit()) {
            case SPADE_SUITS -> "♠";
            case HEART_SUIT -> "♥";
            case ClUB_SUIT -> "♣";
            case DIAMOND_SUIT -> "♦";
            default -> "?";
        };
        String rankStr = switch (c.getRank()) {
            case 14 -> "A";
            case 15 -> "2";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(c.getRank());
        };
        return suitStr + rankStr;
    }

}
