package com.jjg.game.table.redblackwar.util;


import com.jjg.game.table.common.data.Card;
import com.jjg.game.table.redblackwar.constant.HandType;

import java.util.Arrays;
import java.util.List;

/**
 * 牌型比较器
 */
public class CardComparatorUtil {

    /**
     * 牌型等级：豹子(5) > 顺金(4) > 金花(3) > 顺子(2) > 对子(1) > 散牌(0)
     */
    public static HandType getCardType(List<Card> cardList) {
        Card[] cards = cardList.toArray(new Card[0]);
        sortCards(cards);
        if (isLeopard(cards)) return HandType.LEOPARD;
        if (isStraightFlush(cards)) return HandType.STRAIGHT_FLUSH;
        if (isFlush(cards)) return HandType.FLUSH;
        if (isStraight(cards)) return HandType.STRAIGHT;
        if (isPair(cards)) return HandType.PAIR;
        return HandType.HIGH_CARD;
    }

    public static int compareCards(Card[] cards, Card[] cards1, HandType type) {
        return switch (type) {
            case LEOPARD -> compareLeopard(cards, cards1);
            case STRAIGHT_FLUSH, STRAIGHT -> compareStraight(cards, cards1);
            case FLUSH -> compareFlush(cards, cards1);
            case PAIR -> comparePair(cards, cards1);
            default -> compareHighCard(cards, cards1);
        };
    }

    private static int compareLeopard(Card[] cards, Card[] cards1) {
        return cards[0].compare(cards1[0]);
    }

    private static int compareStraight(Card[] cards, Card[] cards1) {
        // QKA顺子特殊处理
        if (isQKA(cards) && isQKA(cards1)) {
            return compareCards(cards, cards1, HandType.HIGH_CARD);
        }
        if (isQKA(cards)) return -1;
        if (isQKA(cards1)) return 1;

        return cards[0].compare(cards1[0]);
    }

    private static int compareFlush(Card[] cards, Card[] cards1) {
        for (int i = 0; i < cards.length; i++) {
            int diff = cards[i].rank - cards1[i].rank;
            if (diff != 0) return diff;
        }
        return cards[0].compare(cards1[0]);
    }

    private static int comparePair(Card[] cards, Card[] cards1) {
        int pairValue = cards[1].rank;
        int pairValue1 = cards1[1].rank;
        int diff = pairValue - pairValue1;

        if (diff == 0) {
            // 比较最大单牌
            int single1 = cards[0].rank > cards[1].rank ? cards[0].rank : cards[2].rank;
            int single2 = cards1[0].rank > cards1[1].rank ? cards1[0].rank : cards1[2].rank;
            diff = single1 - single2;

            if (diff == 0) {
                // 比较最小单牌
                single1 = cards[0].rank < cards[1].rank ? cards[0].rank : cards[2].rank;
                single2 = cards1[0].rank < cards1[1].rank ? cards1[0].rank : cards1[2].rank;
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
            int diff = cards[0].compare(cards1[i], false);
            if (diff != 0) return diff;
        }
        return cards[0].compare(cards1[0]);
    }

    private static boolean isLeopard(Card[] cards) {
        return cards[0].rank == cards[1].rank && cards[1].rank == cards[2].rank;
    }

    private static boolean isStraightFlush(Card[] cards) {
        return isFlush(cards) && isStraight(cards);
    }

    private static boolean isFlush(Card[] cards) {
        return cards[0].getSuit() == cards[1].getSuit() && cards[1].getSuit() == cards[2].getSuit();
    }

    private static boolean isStraight(Card[] cards) {
        return (cards[0].rank - cards[1].rank == 1 && cards[1].rank - cards[2].rank == 1) ||
                isQKA(cards);
    }

    private static boolean isQKA(Card[] cards) {
        return cards[0].rank == 13 && cards[1].rank == 12 && cards[2].rank == 1;
    }

    private static boolean isPair(Card[] cards) {
        return cards[0].rank == cards[1].rank || cards[1].rank == cards[2].rank;
    }

    public static void sortCards(Card[] cards) {
        Arrays.sort(cards, (a, b) -> b.rank - a.rank);
    }
}
