package com.jjg.game.table.redblackwar.util;

import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.table.redblackwar.constant.HandType;
import com.jjg.game.table.redblackwar.data.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * 根据牌型生成扑克牌
 *
 * @author lm
 */
public class PokerHandGenerator {

    private static List<Integer> deck = new ArrayList<>();
    private static List<Integer> suitList = new ArrayList<>();

    static {
        suitList.add(PokerCardUtils.EPokerSuit.DIAMOND.getSuitId() - 1);
        suitList.add(PokerCardUtils.EPokerSuit.CLUBS.getSuitId() - 1);
        suitList.add(PokerCardUtils.EPokerSuit.HEART.getSuitId() - 1);
        suitList.add(PokerCardUtils.EPokerSuit.SPADES.getSuitId() - 1);
    }

    public static List<Card> dealHand(HandType type, List<Integer> deckList) {
        if (deckList.size() < 3) {
            return List.of();
        }
        deck = deckList;
        Collections.shuffle(deck);
        Collections.shuffle(suitList);
        return switch (type) {
            case LEOPARD -> generateLeopard();
            case STRAIGHT_FLUSH -> generateStraightFlush();
            case FLUSH -> generateFlush();
            case STRAIGHT -> generateStraight();
            case PAIR -> generatePair();
            default -> generateHighCard();
        };
    }
    // ----------- 牌型生成方法 -----------

    private static int getCardPoint(int value) {
        return (value - 1) % 13 + 1;
    }

    /**
     * 生成豹子
     */
    private static List<Card> generateLeopard() {
        for (int i = 0; i < deck.size(); i++) {
            int cardPoint = getCardPoint(deck.get(i));
            List<Card> result = new ArrayList<>();
            for (Integer suit : suitList) {
                int value = suit * 13 + cardPoint;
                if (deck.contains(value)) {
                    result.add(new Card(value));
                }
                if (result.size() == 3) {
                    remove(result);
                    return result;
                }
            }
        }
        return generateHighCard();
    }

    /**
     * 生成顺金 12345678910 j(11) q(12) k(13)
     */
    private static List<Card> generateStraightFlush() {
        for (int i = 0; i < deck.size(); i++) {
            int start = getCardPoint(deck.get(i));
            if (start > 12) {
                continue;
            }
            List<Integer> ranks = (start == 12) ? List.of(12, 13, 1) : List.of(start, start + 1, start + 2);
            for (int suit : suitList) {
                List<Card> result = new ArrayList<>();
                for (int rank : ranks) {
                    int val = suit * 13 + rank;
                    if (!deck.contains(val)) {
                        break;
                    }
                    result.add(new Card(val));
                }
                if (result.size() == 3) {
                    remove(result);
                    return result;
                }
            }
        }
        return generateHighCard();

    }

    /**
     * 生成金花 和初始牌值不一样 不连续 花色一样
     */
    private static List<Card> generateFlush() {
        for (int i = 0; i < deck.size(); i++) {
            Card[] temp = new Card[3];
            temp[0] = new Card(deck.get(i));
            for (int j = i + 1; j < deck.size(); j++) {
                temp[1] = new Card(deck.get(j));
                if (temp[0].suit != temp[1].suit || Math.abs(temp[0].rank - temp[1].rank) == 1) {
                    continue;
                }
                for (int k = j + 1; k < deck.size(); k++) {
                    temp[2] = new Card(deck.get(k));
                    if (temp[1].suit != temp[2].suit || Math.abs(temp[0].rank - temp[2].rank) == 1) {
                        continue;
                    }
                    List<Card> list = Arrays.stream(temp).toList();
                    if (!isStraightRanks(list)) {
                        remove(list);
                        return list;
                    }
                }
            }
        }
        return generateHighCard();

    }


    /**
     * 生成顺子
     */
    private static List<Card> generateStraight() {

        for (int i = 0; i < deck.size(); i++) {
            int start = getCardPoint(deck.get(i));
            if (start > 12) {
                continue;
            }
            List<Integer> ranks = (start == 12) ? List.of(12, 13, 1) : List.of(start, start + 1, start + 2);
            for (int k = 0; k < suitList.size(); k++) {
                List<Integer> temp = new ArrayList<>(suitList);
                int val = suitList.get(k) * 13 + ranks.get(0);
                if (deck.contains(val)) {
                    Integer next = ranks.get(1);
                    for (int l = temp.size() - 1; l >= 0; l--) {
                        int nextVal = temp.get(l) * 13 + next;
                        if (deck.contains(nextVal)) {
                            temp.remove(l);
                            Integer last = ranks.get(2);
                            for (int j = temp.size() - 1; j >= 0; j--) {
                                int lastVal = temp.get(j) * 13 + last;
                                if (deck.contains(lastVal)) {
                                    List<Card> cards = List.of(new Card(val), new Card(nextVal), new Card(lastVal));
                                    remove(cards);
                                    return cards;
                                }
                            }
                        }
                    }
                }
            }

        }
        return generateHighCard();
    }

    /**
     * 生成对子
     */
    private static List<Card> generatePair() {
        for (int i = 0; i < deck.size(); i++) {
            Card card = new Card(deck.get(i));
            Card[] temp = new Card[3];
            temp[0] = card;
            for (Integer suit : suitList) {
                if (temp[0].suit == suit) {
                    continue;
                }
                int val = suit * 13 + card.rank;
                if (deck.contains(val)) {
                    temp[1] = new Card(val);
                    break;
                }
            }
            if (temp[1] == null) {
                continue;
            }
            for (int j = i + 1; j < deck.size(); j++) {
                temp[2] = new Card(deck.get(j));
                if (temp[1].getValue() == temp[2].getValue()) {
                    continue;
                }
                if (temp[1].rank == temp[2].rank) {
                    continue;
                }
                List<Card> list = Arrays.stream(temp).toList();
                remove(list);
                return list;
            }
        }
        return generateHighCard();
    }

    /**
     * 生成散牌
     */
    private static List<Card> generateHighCard() {
        for (int i = 0; i < deck.size(); i++) {
            Card[] temp = new Card[3];
            Card card = new Card(deck.get(i));
            temp[0] = card;
            for (int j = i + 1; j < deck.size(); j++) {
                Card next = new Card(deck.get(j));
                temp[1] = next;
                for (int k = j + 1; k < deck.size(); k++) {
                    temp[2] = new Card(deck.get(k));
                    //验证花色
                    boolean valid = !(temp[0].getSuit() == temp[1].getSuit() && temp[0].getSuit() == temp[2].getSuit());
                    valid = valid && (temp[0].getRank() != temp[1].getRank() && temp[0].getRank() != temp[2].getRank() && temp[1].getRank() != temp[2].getRank());
                    //验证点数
                    if (valid) {
                        List<Card> result = Arrays.stream(temp).toList();
                        if (!isStraightRanks(result)) {
                            remove(result);
                            return result;
                        }
                    }
                }
            }
        }
        return List.of();
    }

    // ------------ 工具方法 ------------

    private static void remove(List<Card> cards) {
        for (Card c : cards) deck.remove(Integer.valueOf(c.getValue()));
    }

    private static boolean isStraightRanks(List<Card> cards) {
        List<Integer> ranks = cards.stream().map(c -> c.rank).sorted().toList();
        return (ranks.contains(1) && ranks.contains(12) && ranks.contains(13)) ||
                (ranks.get(1) - ranks.get(0) == 1 && ranks.get(2) - ranks.get(1) == 1);
    }

//    public static void main(String[] args) {
//        LocalDateTime now = LocalDateTime.now();
////        for (int k = 0; k < 100000; k++) {
////            for (HandType first : values()) {
////                for (HandType send : values()) {
//        HandType first = STRAIGHT;
//        HandType send = STRAIGHT;
//        List<Integer> newDeck = new ArrayList<>();
//        for (int i = 1; i <= 52; i++) newDeck.add(i);
//        //        String[] split = "1, 21, 38, 10, 48, 51, 15, 50, 14, 17, 2, 46, 31, 9, 33, 35".split(", ");
//        //        newDeck = Arrays.stream(split).map(Integer::parseInt).collect(Collectors.toList());
//        List<Integer> integers = new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            List<Card> hand1 = dealHand(first, newDeck);
//            List<Card> hand2 = dealHand(send, newDeck);
//            integers.addAll(hand1.stream().map(Card::getValue).toList());
//            integers.addAll(hand2.stream().map(Card::getValue).toList());
//            Card[] hand1Array = hand1.toArray(new Card[0]);
//            HandType handType = CardComparatorUtil.getCardType(hand1Array);
//            Card[] hand2Array = hand2.toArray(new Card[0]);
//            HandType handType2 = CardComparatorUtil.getCardType(hand2Array);
//            if (handType != first && newDeck.size() >= 3) {
//                ArrayList<Integer> integers1 = new ArrayList<>(newDeck);
//                integers1.sort(Collections.reverseOrder());
//                System.out.println("handType:" + first + integers1.stream().map(Card::new).toList());
//            }
//            if (handType2 != send && newDeck.size() >= 3) {
//                ArrayList<Integer> integers1 = new ArrayList<>(newDeck);
//                integers1.sort(Collections.reverseOrder());
//                System.out.println("handType:" + send + integers1.stream().map(Card::new).toList());
//            }
//            System.out.println("Hand1: " + i + "->" + hand1 + " -> " + handType);
//            System.out.println("Hand2: " + i + "->" + hand2 + " -> " + handType2);
//            int result = handType.rank - handType2.rank;
//            if (result == 0) {
//                result = hand1Array.length - hand2Array.length;
//                if (result == 0) {
//                    result = CardComparatorUtil.compareCards(hand1Array, hand2Array, handType);
//                }
//            }
//            System.out.println("结果: " + (result > 0 ? "Hand1 大" : result < 0 ? "Hand2 大" : "平局"));
//            List<Integer> list = integers.stream().sorted().toList();
//            System.out.println("剩余牌" + newDeck);
//            if (hand2.isEmpty() || hand1.isEmpty()) {
//                //                        System.out.println("____________________________________________________________________");
//                break;
//            }
//            System.out.println("已用牌：" + list);
//            System.out.println("总数:" + (list.size() + newDeck.size()));
//            System.out.println("____________________________________________________________________");
//        }
////                }
////            }
////        }
//        System.out.println(ChronoUnit.SECONDS.between(now, LocalDateTime.now()));
//
//    }


}
