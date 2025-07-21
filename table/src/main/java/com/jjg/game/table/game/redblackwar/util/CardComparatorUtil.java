package com.jjg.game.table.game.redblackwar.util;

import com.jjg.game.table.game.redblackwar.data.Card;

import java.util.Arrays;

/**
 * 牌型比较器
 */
public class CardComparatorUtil {
    private static final int[] RANK_ORDER = {12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
    private static final int[] SUIT_ORDER = {3, 2, 1, 0};  // ♠, ♥, ♦, ♣

    /**
     * 牌型等级：豹子(5) > 顺金(4) > 金花(3) > 顺子(2) > 对子(1) > 散牌(0)
     */
    public static int getCardType(Card[] cards) {
        if (isLeopard(cards)) return 5;
        if (isStraightFlush(cards)) return 4;
        if (isFlush(cards)) return 3;
        if (isStraight(cards)) return 2;
        if (isPair(cards)) return 1;
        return 0;
    }

    public static int compareCards(Card[] cards, Card[] cards1, int type) {
        return switch (type) {
            case 5 -> compareLeopard(cards, cards1);
            case 4, 2 -> compareStraight(cards, cards1);
            case 3 -> compareFlush(cards, cards1);
            case 1 -> comparePair(cards, cards1);
            default -> compareHighCard(cards, cards1);
        };
    }

    private static int compareLeopard(Card[] cards, Card[] cards1) {
        return cards[0].getValue() - cards1[0].getValue();
    }

    private static int compareStraight(Card[] cards, Card[] cards1) {
        // A23顺子特殊处理
        if (isA23(cards) && isA23(cards1)) {
            return compareCards(cards, cards1, 0);
        }
        if (isA23(cards)) return -1;
        if (isA23(cards1)) return 1;

        return cards[0].getValue() - cards1[0].getValue();
    }

    private static int compareFlush(Card[] cards, Card[] cards1) {
        for (int i = 0; i < cards.length; i++) {
            int diff = cards[i].getValue() - cards1[i].getValue();
            if (diff != 0) return diff;
        }
        return cards[0].compare(cards1[0]);
    }

    private static int comparePair(Card[] cards, Card[] cards1) {
        int pairValue = cards[1].getValue();
        int pairValue1 = cards1[1].getValue();
        int diff = pairValue - pairValue1;

        if (diff == 0) {
            // 比较最大单牌
            int single1 = cards[0].getValue() > cards[1].getValue() ? cards[0].getValue() : cards[2].getValue();
            int single2 = cards1[0].getValue() > cards1[1].getValue() ? cards1[0].getValue() : cards1[2].getValue();
            diff = single1 - single2;

            if (diff == 0) {
                // 比较最小单牌
                single1 = cards[0].getValue() < cards[1].getValue() ? cards[0].getValue() : cards[2].getValue();
                single2 = cards1[0].getValue() < cards1[1].getValue() ? cards1[0].getValue() : cards1[2].getValue();
                diff = single1 - single2;

                if (diff == 0) {
                    // 比较对子的花色
                    diff = cards[1].compare(cards1[1]);
                }
            }
        }
        return diff;
    }

    private static int compareHighCard(Card[] cards, Card[] cards1) {
        for (int i = 0; i < cards.length; i++) {
            int diff = cards[i].getValue() - cards1[i].getValue();
            if (diff != 0) return diff;
        }
        return 0;
    }

    private static boolean isLeopard(Card[] cards) {
        return cards[0].getValue() == cards[1].getValue() && cards[1].getValue() == cards[2].getValue();
    }

    private static boolean isStraightFlush(Card[] cards) {
        return isFlush(cards) && isStraight(cards);
    }

    private static boolean isFlush(Card[] cards) {
        return cards[0].suitValue() == cards[1].suitValue() && cards[1].suitValue() == cards[2].suitValue();
    }

    private static boolean isStraight(Card[] cards) {
        return (cards[0].getValue() - cards[1].getValue() == 1 && cards[1].getValue() - cards[2].getValue() == 1) ||
               isA23(cards);
    }

    private static boolean isA23(Card[] cards) {
        return cards[0].getValue() == 13 && cards[1].getValue() == 2 && cards[2].getValue() == 1;
    }

    private static boolean isPair(Card[] cards) {
        return cards[0].getValue() == cards[1].getValue() || cards[1].getValue() == cards[2].getValue();
    }

    public static void sortCards(Card[] cards) {
        Arrays.sort(cards, (a, b) -> b.getValue() - a.getValue());
    }
}
