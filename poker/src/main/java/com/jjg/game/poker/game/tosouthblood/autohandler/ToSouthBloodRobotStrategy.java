package com.jjg.game.poker.game.tosouthblood.autohandler;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.tosouthblood.room.data.ToSouthBloodGameDataVo;
import com.jjg.game.poker.game.tosouthblood.util.ToSouthBloodCardType;
import com.jjg.game.poker.game.tosouthblood.util.ToSouthBloodHandUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant.*;

/**
 * 南方前进-血战机器人出牌策略
 * <p>
 * 无状态工具类，所有方法均为 static。
 * 只负责决策"出哪些牌"，不修改任何游戏状态。
 * <p>
 * 复用 {@link ToSouthBloodHandUtils} 的工具方法（integrateHandCards / findAllFollowPlays 等），
 * 不改动游戏规则。
 */
public class ToSouthBloodRobotStrategy {

    private static final Logger log = LoggerFactory.getLogger(ToSouthBloodRobotStrategy.class);

    /** Q 的 rank 值 */
    private static final int RANK_Q = 12;
    /** 10 的 rank 值 */
    private static final int RANK_10 = 10;

    // ===================== 手牌分析缓存 =====================

    /**
     * 预计算手牌分析结果，避免多处重复调用 integrateHandCards / findAllBestPlays
     */
    private static class HandContext {
        final List<Card> handCards;
        final int handSize;
        final Map<Integer, List<Card>> rankMap;
        final Map<ToSouthBloodCardType, List<List<Card>>> integrated;

        final List<List<Card>> bombs;
        final List<List<Card>> straights;
        final List<List<Card>> triples;
        final List<List<Card>> pairs;
        final List<List<Card>> singles;
        final List<List<Card>> consecPairs;

        final int twoCount;

        HandContext(List<Card> handCards) {
            this.handCards = new ArrayList<>(handCards);
            this.handSize = handCards.size();
            this.rankMap = ToSouthBloodHandUtils.convertCardListToRankMap(handCards);
            this.integrated = ToSouthBloodHandUtils.integrateHandCards(handCards);

            this.bombs = getOrEmpty(integrated, ToSouthBloodCardType.BOMB_QUAD);
            this.straights = getOrEmpty(integrated, ToSouthBloodCardType.STRAIGHT);
            this.triples = getOrEmpty(integrated, ToSouthBloodCardType.TRIPLE);
            this.pairs = getOrEmpty(integrated, ToSouthBloodCardType.PAIR);
            this.singles = getOrEmpty(integrated, ToSouthBloodCardType.SINGLE);
            this.consecPairs = getOrEmpty(integrated, ToSouthBloodCardType.CONSECUTIVE_PAIRS);

            // 统计2的数量
            int cnt = 0;
            for (Card c : handCards) {
                if (c.getRank() == RANK_2) cnt++;
            }
            this.twoCount = cnt;
        }

        private static List<List<Card>> getOrEmpty(Map<ToSouthBloodCardType, List<List<Card>>> map, ToSouthBloodCardType type) {
            List<List<Card>> list = map.get(type);
            return list != null ? list : Collections.emptyList();
        }
    }

    // ===================== 公开入口 =====================

    /**
     * 机器人主动出牌（首出/领出）
     *
     * @param handCards  手牌
     * @param gameDataVo 游戏状态
     * @param mySeatInfo 机器人座位信息
     * @return 要出的牌，null 表示过牌（首出理论上不会返回 null）
     */
    public static List<Card> chooseLeaderPlay(List<Card> handCards, ToSouthBloodGameDataVo gameDataVo, PlayerSeatInfo mySeatInfo) {
        if (CollUtil.isEmpty(handCards)) return null;
        HandContext ctx = new HandContext(handCards);

        // 1. 首轮必须出黑桃3
        if (gameDataVo.isFirstRound()) {
            List<Card> play = handleFirstRoundLeader(ctx, handCards);
            if (play != null) return play;
        }

        // 2. 有对手报单（手牌==1）→ 优先出多张牌型封堵
        if (hasAlertPlayer(gameDataVo, mySeatInfo)) {
            List<Card> play = handleAlertLeader(ctx);
            if (play != null) return play;
        }

        // 3. 炸弹清场：手牌 = 炸弹 + 1组其他牌 → 先出炸弹
        List<Card> bombClear = tryBombClearingPlay(ctx);
        if (bombClear != null) return bombClear;

        // 4. 全场所有人手牌 < 5 张 → 优先打2
        if (allPlayersUnder5Cards(gameDataVo)) {
            List<Card> twoPlay = tryPlayTwos(ctx);
            if (twoPlay != null) return twoPlay;
        }

        // 5. 正常优先级出牌
        return normalLeaderPlay(ctx);
    }

    /**
     * 机器人跟牌
     *
     * @param handCards  手牌
     * @param lastCards  上家出的牌
     * @param gameDataVo 游戏状态
     * @param mySeatInfo 机器人座位信息
     * @return 要出的牌，null 表示过牌
     */
    public static List<Card> chooseFollowPlay(List<Card> handCards, List<Card> lastCards,
                                              ToSouthBloodGameDataVo gameDataVo, PlayerSeatInfo mySeatInfo) {
        if (CollUtil.isEmpty(handCards) || CollUtil.isEmpty(lastCards)) return null;

        ToSouthBloodCardType lastType = ToSouthBloodHandUtils.getCardType(lastCards);
        if (lastType == ToSouthBloodCardType.NONE) return null;

        HandContext ctx = new HandContext(handCards);
        int lastPlaySeatId = gameDataVo.getLastPlaySeatId();
        int opponentHandSize = getPlayerHandSize(gameDataVo, lastPlaySeatId);

        // 特殊规则：手中有炸弹 && 上家出的是2或炸弹 → 必须炸
        if (hasBomb(ctx) && isLastPlayTwoOrBomb(lastCards, lastType)) {
            List<Card> bombPlay = followWithBomb(ctx, lastCards);
            if (bombPlay != null) return bombPlay;
        }

        return switch (lastType) {
            case STRAIGHT -> followStraight(ctx, lastCards);
            case TRIPLE -> followTriple(ctx, lastCards, opponentHandSize);
            case PAIR -> followPair(ctx, lastCards, opponentHandSize);
            case SINGLE -> followSingle(ctx, lastCards);
            case BOMB_QUAD -> followBomb(ctx, lastCards);
            case CONSECUTIVE_PAIRS -> followConsecutivePairs(ctx, lastCards);
            default -> null;
        };
    }

    // ===================== 主动出牌子策略 =====================

    /**
     * 首轮必须出黑桃3的处理
     */
    private static List<Card> handleFirstRoundLeader(HandContext ctx, List<Card> handCards) {
        Card spade3 = findSpade3(handCards);
        if (spade3 == null) {
            // 容错：手里没黑桃3（理论不会出现），普通出牌
            return ToSouthBloodHandUtils.findBestPlay(handCards);
        }

        Map<Integer, List<Card>> rankMap = ToSouthBloodHandUtils.convertCardListToRankMap(handCards);
        List<Card> rank3Cards = rankMap.get(RANK_3);

        // 特例1：有4个3（含黑桃3）→ 出炸弹
        if (rank3Cards != null && rank3Cards.size() == 4) {
            return new ArrayList<>(rank3Cards);
        }

        // 默认：用现有的 findBestPlayWithFirstCard 找包含黑桃3的最佳组合
        // 该方法优先级：炸弹 > 三张 > 连对/顺子 > 对子 > 单张
        // 已经会优先单出黑桃3（如果3只有1张的话），避免浪费大牌
        return ToSouthBloodHandUtils.findBestPlayWithFirstCard(rankMap, spade3);
    }

    /**
     * 有对手报单时的出牌策略
     * 优先出非单张牌型（顺子 > 三张 > 对子），封堵1张牌的对手
     * 都没有 → 出最大单张
     */
    private static List<Card> handleAlertLeader(HandContext ctx) {
        // 优先出顺子 > 三张 > 对子（这些牌型1张牌的对手无法跟）
        if (CollUtil.isNotEmpty(ctx.straights)) {
            return getSmallest(ctx.straights);
        }
        if (CollUtil.isNotEmpty(ctx.triples)) {
            return getSmallest(ctx.triples);
        }
        if (CollUtil.isNotEmpty(ctx.pairs)) {
            return getSmallest(ctx.pairs);
        }

        // 只剩单张 → 出最大的
        if (CollUtil.isNotEmpty(ctx.singles)) {
            return getLargest(ctx.singles);
        }

        // 连对/炸弹兜底
        if (CollUtil.isNotEmpty(ctx.consecPairs)) {
            return getSmallest(ctx.consecPairs);
        }
        if (CollUtil.isNotEmpty(ctx.bombs)) {
            return getSmallest(ctx.bombs);
        }

        return null;
    }

    /**
     * 炸弹清场检测
     * 手牌 = N个炸弹 + 恰好1组其他牌（能一次出完）→ 先出炸弹
     * 手牌全是炸弹 → 出最小炸弹
     */
    private static List<Card> tryBombClearingPlay(HandContext ctx) {
        if (ctx.bombs.isEmpty() && ctx.consecPairs.isEmpty()) return null;

        // 统计非炸弹牌组数
        int nonBombGroupCount = 0;
        nonBombGroupCount += ctx.straights.size();
        nonBombGroupCount += ctx.triples.size();
        nonBombGroupCount += ctx.pairs.size();
        nonBombGroupCount += ctx.singles.size();

        // 手牌全是炸弹（四条+连对） → 出最小四条炸弹
        if (nonBombGroupCount == 0) {
            if (!ctx.bombs.isEmpty()) return getSmallest(ctx.bombs);
            if (!ctx.consecPairs.isEmpty()) return getSmallest(ctx.consecPairs);
        }

        // 手牌 = 炸弹 + 恰好1组其他牌 → 先出炸弹
        if (nonBombGroupCount == 1) {
            if (!ctx.bombs.isEmpty()) return getSmallest(ctx.bombs);
            // 如果只有连对炸弹，也算
            if (ctx.consecPairs.size() > 1) return getSmallest(ctx.consecPairs);
        }

        return null;
    }

    /**
     * 全场手牌 < 5 时，优先打出2
     */
    private static List<Card> tryPlayTwos(HandContext ctx) {
        if (ctx.twoCount == 0) return null;

        List<Card> twos = ctx.rankMap.get(RANK_2);
        if (twos == null || twos.isEmpty()) return null;

        // 按数量打出2：3个2→三张，2个2→对子，1个2→单张
        twos = new ArrayList<>(twos);
        twos.sort(ToSouthBloodHandUtils.CARD_COMPARATOR);
        if (twos.size() >= 3) return new ArrayList<>(twos.subList(0, 3));
        if (twos.size() == 2) return new ArrayList<>(twos.subList(0, 2));
        return List.of(twos.getFirst());
    }

    /**
     * 正常优先级出牌
     * ① 顺子 → ② 三张（条件跳过）→ ③ 对子（条件跳过）→ ④ 单张
     */
    private static List<Card> normalLeaderPlay(HandContext ctx) {
        // ① 顺子（最难出，优先清）
        if (CollUtil.isNotEmpty(ctx.straights)) {
            return getSmallest(ctx.straights);
        }

        // ② 三张
        if (CollUtil.isNotEmpty(ctx.triples)) {
            List<Card> smallestTriple = getSmallest(ctx.triples);
            int tripleRank = smallestTriple.getFirst().getRank();

            // 条件跳过：rank > Q(12) && 手牌 > 5张 && 有rank < Q的对子或单张
            boolean shouldSkip = tripleRank > RANK_Q
                    && ctx.handSize > 5
                    && hasSmallPairsOrSingles(ctx, RANK_Q);

            if (!shouldSkip) {
                return smallestTriple;
            }
            // 跳过三张，继续往下看对子/单张
        }

        // ③ 对子
        if (CollUtil.isNotEmpty(ctx.pairs)) {
            List<Card> smallestPair = getSmallest(ctx.pairs);
            int pairRank = smallestPair.getFirst().getRank();

            // 条件跳过：rank > 10 && 有rank < 10的单张
            if (pairRank > RANK_10 && hasSinglesBelow(ctx, RANK_10)) {
                // 跳过大对子，出最小单张
                return getSmallest(ctx.singles);
            }
            return smallestPair;
        }

        // ④ 单张
        if (CollUtil.isNotEmpty(ctx.singles)) {
            return getSmallest(ctx.singles);
        }

        // 兜底：连对 → 炸弹
        if (CollUtil.isNotEmpty(ctx.consecPairs)) return getSmallest(ctx.consecPairs);
        if (CollUtil.isNotEmpty(ctx.bombs)) return getSmallest(ctx.bombs);

        // 极端兜底：出最小的一张
        List<Card> sorted = new ArrayList<>(ctx.handCards);
        sorted.sort(ToSouthBloodHandUtils.CARD_COMPARATOR);
        return List.of(sorted.getLast());
    }

    // ===================== 跟牌子策略 =====================

    /**
     * 接顺子：能接就接，过牌不犹豫
     */
    private static List<Card> followStraight(HandContext ctx, List<Card> lastCards) {
        List<List<Card>> allFollows = ToSouthBloodHandUtils.findAllFollowPlays(ctx.handCards, lastCards);
        List<List<Card>> straightFollows = filterByType(allFollows, ToSouthBloodCardType.STRAIGHT);
        if (!straightFollows.isEmpty()) {
            return straightFollows.getFirst(); // 最小的顺子
        }
        return null;
    }

    /**
     * 接三张
     * 一般：从小到大出
     * 特判（自己有3个2）：保护大牌，条件性过牌
     */
    private static List<Card> followTriple(HandContext ctx, List<Card> lastCards, int opponentHandSize) {
        List<List<Card>> allFollows = ToSouthBloodHandUtils.findAllFollowPlays(ctx.handCards, lastCards);
        List<List<Card>> tripleFollows = filterByType(allFollows, ToSouthBloodCardType.TRIPLE);

        if (tripleFollows.isEmpty()) return null;

        int lastRank = lastCards.getFirst().getRank();
        boolean hasTripleTwo = ctx.twoCount >= 3;

        if (hasTripleTwo) {
            // 先看有没有非2的三张能接
            List<List<Card>> nonTwoTriples = tripleFollows.stream()
                    .filter(t -> t.getFirst().getRank() != RANK_2)
                    .collect(Collectors.toList());

            if (!nonTwoTriples.isEmpty()) {
                return nonTwoTriples.getFirst(); // 出最小的非2三张
            }

            // 只剩3×2能接，应用特殊规则
            // 对方rank > 10 → 必须接
            if (lastRank > RANK_10) {
                return tripleFollows.getFirst();
            }

            // 对方rank ≤ 10
            if (opponentHandSize < 5) {
                // 对方快跑了 → 必须接
                return tripleFollows.getFirst();
            }

            // 对方手牌 ≥ 5
            if (ctx.handSize > 5 && hasSmallPairsOrSingles(ctx, RANK_10)) {
                return null; // 过牌，保留3×2
            }
            if (ctx.handSize <= 5) {
                return tripleFollows.getFirst(); // 为了跑牌，必须接
            }

            return null; // 默认过牌
        }

        // 没有3×2：出最小的三张
        return tripleFollows.getFirst();
    }

    /**
     * 接对子
     * 一般：从小到大出
     * 特判（自己有2个2）：保护大牌，条件性过牌
     */
    private static List<Card> followPair(HandContext ctx, List<Card> lastCards, int opponentHandSize) {
        List<List<Card>> allFollows = ToSouthBloodHandUtils.findAllFollowPlays(ctx.handCards, lastCards);
        List<List<Card>> pairFollows = filterByType(allFollows, ToSouthBloodCardType.PAIR);

        if (pairFollows.isEmpty()) return null;

        int lastRank = lastCards.getFirst().getRank();
        boolean hasPairTwo = ctx.twoCount >= 2;

        if (hasPairTwo) {
            // 先看有没有非2的对子能接
            List<List<Card>> nonTwoPairs = pairFollows.stream()
                    .filter(p -> p.getFirst().getRank() != RANK_2)
                    .collect(Collectors.toList());

            if (!nonTwoPairs.isEmpty()) {
                return nonTwoPairs.getFirst(); // 出最小的非2对子
            }

            // 只剩对2能接，应用特殊规则
            if (lastRank > RANK_10) {
                return pairFollows.getFirst(); // 必须接
            }

            if (opponentHandSize < 5) {
                return pairFollows.getFirst(); // 对方快跑了，必须接
            }

            // 对方手牌 ≥ 5
            if (ctx.handSize > 5 && hasSmallSinglesOrPairs(ctx, RANK_10)) {
                return null; // 过牌，保留对2
            }
            if (ctx.handSize <= 5) {
                return pairFollows.getFirst(); // 为了跑牌，必须接
            }

            return null; // 默认过牌
        }

        // 没有对2：出最小的对子
        return pairFollows.getFirst();
    }

    /**
     * 接单张：能接就接，出最大单张
     * 因为单张迟早要出，不如现在用来管牌
     */
    private static List<Card> followSingle(HandContext ctx, List<Card> lastCards) {
        List<List<Card>> allFollows = ToSouthBloodHandUtils.findAllFollowPlays(ctx.handCards, lastCards);
        List<List<Card>> singleFollows = filterByType(allFollows, ToSouthBloodCardType.SINGLE);

        if (singleFollows.isEmpty()) return null;

        // findAllFollowPlays 排序是升序（小在前），取最后一个 = 最大单张
        return singleFollows.getLast();
    }

    /**
     * 接炸弹：有更大的炸弹才接
     * 多个可接 → 出最小的（保留高倍炸弹）
     * 只有一个 → 直接出
     */
    private static List<Card> followBomb(HandContext ctx, List<Card> lastCards) {
        List<List<Card>> allFollows = ToSouthBloodHandUtils.findAllFollowPlays(ctx.handCards, lastCards);
        if (allFollows.isEmpty()) return null;

        // findAllFollowPlays 已经排好序（小炸弹在前）
        return allFollows.getFirst();
    }

    /**
     * 接连对：能接就接
     */
    private static List<Card> followConsecutivePairs(HandContext ctx, List<Card> lastCards) {
        List<List<Card>> allFollows = ToSouthBloodHandUtils.findAllFollowPlays(ctx.handCards, lastCards);
        List<List<Card>> cpFollows = filterByType(allFollows, ToSouthBloodCardType.CONSECUTIVE_PAIRS);
        if (!cpFollows.isEmpty()) {
            return cpFollows.getFirst();
        }
        return null;
    }

    /**
     * 特殊规则：手中有炸弹 && 上家出了2或炸弹 → 必须炸
     * 多个可接炸弹 → 出最小倍数/点数的
     */
    private static List<Card> followWithBomb(HandContext ctx, List<Card> lastCards) {
        List<List<Card>> allFollows = ToSouthBloodHandUtils.findAllFollowPlays(ctx.handCards, lastCards);
        if (allFollows.isEmpty()) return null;

        // 过滤只保留炸弹类型的跟牌（四条炸弹 + 连对炸弹）
        List<List<Card>> bombFollows = allFollows.stream()
                .filter(f -> {
                    ToSouthBloodCardType t = ToSouthBloodHandUtils.getCardType(f);
                    return t == ToSouthBloodCardType.BOMB_QUAD || t == ToSouthBloodCardType.CONSECUTIVE_PAIRS;
                })
                .collect(Collectors.toList());

        if (bombFollows.isEmpty()) return null;

        // findAllFollowPlays 排序：四条小 → 四条大 → 连对，直接取第一个就是最小的
        return bombFollows.getFirst();
    }

    // ===================== 辅助方法 =====================

    /**
     * 判断上家出的牌是否为2的牌型或炸弹
     * 炸弹只能炸2或炸弹，所以只有这些情况下才触发"必须炸"
     */
    private static boolean isLastPlayTwoOrBomb(List<Card> lastCards, ToSouthBloodCardType lastType) {
        // 炸弹类型
        if (lastType == ToSouthBloodCardType.BOMB_QUAD || lastType == ToSouthBloodCardType.CONSECUTIVE_PAIRS) {
            return true;
        }
        // 2的牌型（单2、对2、三个2）
        return lastCards.getFirst().getRank() == RANK_2;
    }

    /** 手中是否有炸弹（四条或连对炸弹） */
    private static boolean hasBomb(HandContext ctx) {
        return !ctx.bombs.isEmpty() || !ctx.consecPairs.isEmpty();
    }

    /** 查找手牌中的黑桃3 */
    private static Card findSpade3(List<Card> handCards) {
        return handCards.stream()
                .filter(c -> c.getRank() == RANK_3 && c.getSuit() == SPADE_SUIT)
                .findFirst()
                .orElse(null);
    }

    /** 获取指定座位玩家的手牌数量 */
    private static int getPlayerHandSize(ToSouthBloodGameDataVo gameDataVo, int seatId) {
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getSeatId() == seatId && !info.isDelState() && !info.isOver()) {
                return info.getCurrentCards().size();
            }
        }
        return 13; // 默认
    }

    /** 是否有对手手牌只剩1张（报单状态） */
    private static boolean hasAlertPlayer(ToSouthBloodGameDataVo gameDataVo, PlayerSeatInfo mySeatInfo) {
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.getSeatId() != mySeatInfo.getSeatId()
                    && !info.isDelState() && !info.isOver()
                    && info.getCurrentCards().size() == 1) {
                return true;
            }
        }
        return false;
    }

    /** 全场所有人手牌 < 5 张 */
    private static boolean allPlayersUnder5Cards(ToSouthBloodGameDataVo gameDataVo) {
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (!info.isDelState() && !info.isOver() && info.getCurrentCards().size() >= 5) {
                return false;
            }
        }
        return true;
    }

    /**
     * 手中是否有 rank < threshold 的对子或单张
     * 用于判断是否应该跳过大三张/大对子，先出小牌
     */
    private static boolean hasSmallPairsOrSingles(HandContext ctx, int threshold) {
        for (List<Card> pair : ctx.pairs) {
            if (!pair.isEmpty() && pair.getFirst().getRank() < threshold) return true;
        }
        for (List<Card> single : ctx.singles) {
            if (!single.isEmpty() && single.getFirst().getRank() < threshold) return true;
        }
        return false;
    }

    /**
     * 手中是否有 rank < threshold 的单张
     */
    private static boolean hasSinglesBelow(HandContext ctx, int threshold) {
        for (List<Card> single : ctx.singles) {
            if (!single.isEmpty() && single.getFirst().getRank() < threshold) return true;
        }
        return false;
    }

    /**
     * 手中是否有 rank < threshold 的单张或对子
     * 用于跟牌时判断是否应该保留大牌
     */
    private static boolean hasSmallSinglesOrPairs(HandContext ctx, int threshold) {
        return hasSmallPairsOrSingles(ctx, threshold);
    }

    /**
     * 按牌型过滤跟牌候选列表
     */
    private static List<List<Card>> filterByType(List<List<Card>> plays, ToSouthBloodCardType type) {
        return plays.stream()
                .filter(p -> ToSouthBloodHandUtils.getCardType(p) == type)
                .collect(Collectors.toList());
    }

    /**
     * 取一组列表中 rank 最小的一组
     * integrateHandCards 返回的组内已按 CARD_COMPARATOR 排序（降序），
     * 所以 getFirst() 是该组最大的牌（代表组的大小）
     */
    private static List<Card> getSmallest(List<List<Card>> groups) {
        if (groups == null || groups.isEmpty()) return null;
        // 按组内最大牌的 rank 升序，rank 相同按花色
        return groups.stream()
                .min((a, b) -> {
                    Card maxA = a.getFirst();
                    Card maxB = b.getFirst();
                    // 用 CARD_COMPARATOR 的反向：CARD_COMPARATOR 是降序的，
                    // compare(A, B) < 0 表示 A 更大，我们要找最小的
                    return ToSouthBloodHandUtils.CARD_COMPARATOR.compare(maxB, maxA);
                })
                .orElse(null);
    }

    /**
     * 取一组列表中 rank 最大的一组
     */
    private static List<Card> getLargest(List<List<Card>> groups) {
        if (groups == null || groups.isEmpty()) return null;
        return groups.stream()
                .max((a, b) -> {
                    Card maxA = a.getFirst();
                    Card maxB = b.getFirst();
                    return ToSouthBloodHandUtils.CARD_COMPARATOR.compare(maxB, maxA);
                })
                .orElse(null);
    }
}
