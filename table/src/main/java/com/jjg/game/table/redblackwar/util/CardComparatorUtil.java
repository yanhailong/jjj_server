package com.jjg.game.table.redblackwar.util;


import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.table.redblackwar.constant.HandType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 牌型比较器
 */
public class CardComparatorUtil {
    public final static Card[] SAMPLE = new Card[0];

    /**
     * 牌型等级：豹子(5) > 顺金(4) > 金花(3) > 顺子(2) > 对子(1) > 散牌(0)
     */
    public static HandType getCardType(Card[] cards) {
        sortCardsAIsMin(cards);
        if (isLeopard(cards)) return HandType.LEOPARD;
        if (isStraightFlush(cards)) return HandType.STRAIGHT_FLUSH;
        if (isFlush(cards)) return HandType.FLUSH;
        if (isStraight(cards)) return HandType.STRAIGHT;
        if (isPair(cards)) return HandType.PAIR;
        return HandType.HIGH_CARD;
    }

    public static int compareCards(HandType handType, Card[] cards, HandType otherHandtype, Card[] otherCards) {
        int result = handType.comper(otherHandtype);
        if (result == 0) {
            result = CardComparatorUtil.compareCards(cards, otherCards, handType, true);
        }
        return Integer.compare(result, 0);
    }


    private static int compareCards(Card[] cards, Card[] cards1, HandType type, boolean sored) {
        if (sored) {
            sortCardsAisMax(cards);
            sortCardsAisMax(cards1);
        }
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
            return compareCards(cards, cards1, HandType.HIGH_CARD, false);
        }
        if (isQKA(cards)) return 1;
        if (isQKA(cards1)) return -1;
        sortCardsAIsMin(cards);
        sortCardsAIsMin(cards1);
        return cards[0].compareAisMin(cards1[0], true);
    }

    private static int compareFlush(Card[] cards, Card[] cards1) {
        for (int i = 0; i < cards.length; i++) {
            int diff = cards[i].compare(cards1[i], false);
            if (diff != 0) return diff;
        }
        return cards[0].compare(cards1[0]);
    }

    private static int comparePair(Card[] cards, Card[] cards1) {
        int diff = cards[1].compare(cards1[1], false);
        if (diff == 0) {
            // 比较最大单牌
            Card cardSingle1 = cards[0].compare(cards[1], false) == 0 ? cards[2] : cards[0];
            Card cardSingle2 = cards1[0].compare(cards1[1], false) == 0 ? cards1[2] : cards1[0];
            return cardSingle1.compare(cardSingle2);
        }
        return diff;
    }

    private static int compareHighCard(Card[] cards, Card[] cards1) {
        for (int i = 0; i < cards.length; i++) {
            int diff = cards[i].compare(cards1[i], false);
            if (diff != 0) return diff;
        }
        return cards[0].compare(cards1[0]);
    }

    private static boolean isLeopard(Card[] cards) {
        return cards[0].getRank() == cards[1].getRank() && cards[1].getRank() == cards[2].getRank();
    }

    private static boolean isStraightFlush(Card[] cards) {
        return isFlush(cards) && isStraight(cards);
    }

    private static boolean isFlush(Card[] cards) {
        return cards[0].getSuit() == cards[1].getSuit() && cards[1].getSuit() == cards[2].getSuit();
    }

    private static boolean isStraight(Card[] cards) {
        return (cards[0].getRank() - cards[1].getRank() == 1 && cards[1].getRank() - cards[2].getRank() == 1) ||
                isQKA(cards);
    }

    private static boolean isQKA(Card[] cards) {
        return cards[0].getRank() == 13 && cards[1].getRank() == 12 && cards[2].getRank() == 1;
    }

    private static boolean isPair(Card[] cards) {
        return cards[0].getRank() == cards[1].getRank() || cards[1].getRank() == cards[2].getRank();
    }

    public static void sortCardsAisMax(Card[] cards) {
        Arrays.sort(cards, (a, b) -> {
            int tempA = a.getRank() == 1 ? 14 : a.getRank();
            int tempB = b.getRank() == 1 ? 14 : b.getRank();
            return tempB - tempA;
        });
    }

    public static void sortCardsAIsMin(Card[] cards) {
        Arrays.sort(cards, (a, b) -> b.getRank() - a.getRank());
    }

    public static void main(String[] args) {
        //获胜方 红 	red:牌型[FLUSH] 	card[[♠8, ♠6, ♠4]] 	black:牌型[FLUSH] 	card:[[♥10, ♥Q, ♥A]]
        for (int i = 0; i < 100; i++) {
            //根据牌型获得牌
            List<Integer> joker = PokerCardUtils.getPokerIntIdExceptJoker();
            Collections.shuffle(joker);
            //取红方的牌
            List<Card> redCard = List.of(new Card(1, 2), new Card(2, 2), new Card(3, 2));
            Card[] redCardArr = redCard.toArray(CardComparatorUtil.SAMPLE);
            //红方牌型
            HandType redHandType = CardComparatorUtil.getCardType(redCardArr);
            //取黑方的牌
            List<Card> blackCard = List.of(new Card(1, 7), new Card(2, 7), new Card(3, 7));
            Card[] blackCardArr = blackCard.toArray(CardComparatorUtil.SAMPLE);
            //黑方牌型
            HandType blackHandType = CardComparatorUtil.getCardType(blackCardArr);
            //比较牌大小
            int result = CardComparatorUtil.compareCards(redHandType, redCardArr, blackHandType, blackCardArr);
            //通知
            System.out.printf("""
                            获胜方 %s \tred:牌型[%s] \tcard[%s] \tblack:牌型[%s] \tcard:[%s]%n""", result == 1 ? "红" : "黑",
                    redHandType, redCard, blackHandType, blackCard);

        }
    }
}
