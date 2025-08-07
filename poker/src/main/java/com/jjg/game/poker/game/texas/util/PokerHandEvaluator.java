package com.jjg.game.poker.game.texas.util;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.data.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PokerHandEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(PokerHandEvaluator.class);

    public static HandResult evaluateBestHand(List<Card> cards) {
        if (cards.size() < 5) {
            return new HandResult(HandRank.HIGH_CARD, new ArrayList<>(), cards);
        }
        List<List<Card>> allCombos = combinations(cards, 5);
        HandResult best = null;
        for (List<Card> combo : allCombos) {
            HandResult res = evaluateHand(combo);
            if (best == null || res.compareTo(best) > 0) {
                best = res;
            }
        }
        return best;
    }

    private static HandResult evaluateHand(List<Card> hand) {
        hand.sort((a, b) -> Integer.compare(b.getRank(), a.getRank()));

        boolean flush = hand.stream().allMatch(c -> c.getSuit() == hand.get(0).getSuit());
        List<Integer> ranks = hand.stream().map(Card::getRank).collect(Collectors.toList());

        boolean straight = isStraight(ranks);
        if (straight && flush) {
            if (ranks.get(0) == 14 && ranks.get(1) == 13) {
                return new HandResult(HandRank.ROYAL_FLUSH, ranks, hand);
            }
            return new HandResult(HandRank.STRAIGHT_FLUSH, ranks, hand);
        }

        Map<Integer, Long> counts = ranks.stream()
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

        List<Integer> sorted = counts.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getValue(), a.getValue());
                    if (cmp == 0) return Integer.compare(b.getKey(), a.getKey());
                    return cmp;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (counts.containsValue(4L)) {
            return new HandResult(HandRank.FOUR_OF_A_KIND, sorted, hand);
        }
        if (counts.containsValue(3L) && counts.containsValue(2L)) {
            return new HandResult(HandRank.FULL_HOUSE, sorted, hand);
        }
        if (flush) {
            return new HandResult(HandRank.FLUSH, ranks, hand);
        }
        if (straight) {
            return new HandResult(HandRank.STRAIGHT, ranks, hand);
        }
        if (counts.containsValue(3L)) {
            return new HandResult(HandRank.THREE_OF_A_KIND, sorted, hand);
        }
        long pairs = counts.values().stream().filter(v -> v == 2L).count();
        if (pairs >= 2) {
            return new HandResult(HandRank.TWO_PAIR, sorted, hand);
        }
        if (pairs == 1) {
            return new HandResult(HandRank.ONE_PAIR, sorted, hand);
        }

        return new HandResult(HandRank.HIGH_CARD, ranks, hand);
    }

    private static boolean isStraight(List<Integer> sortedRanks) {
        List<Integer> unique = sortedRanks.stream().distinct().toList();
        if (unique.size() < 5) return false;

        for (int i = 0; i <= unique.size() - 5; i++) {
            int high = unique.get(i);
            if (unique.get(i + 4) == high - 4) return true;
        }

        // Special A-2-3-4-5
        return unique.contains(14) && new HashSet<>(unique).containsAll(List.of(2, 3, 4, 5));
    }

    private static List<List<Card>> combinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        combineHelper(cards, 0, k, new ArrayList<>(), result);
        return result;
    }

    private static void combineHelper(List<Card> cards, int start, int k, List<Card> temp, List<List<Card>> result) {
        if (temp.size() == k) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i <= cards.size() - (k - temp.size()); i++) {
            temp.add(cards.get(i));
            combineHelper(cards, i + 1, k, temp, result);
            temp.remove(temp.size() - 1);
        }
    }

    public static List<Pair<Long, HandResult>> findWinners(List<PlayerHand> players, List<Card> communityCards) {
        List<Pair<Long, HandResult>> winners = new ArrayList<>();
        HandResult bestResult = null;

        for (PlayerHand player : players) {
            List<Card> allCards = new ArrayList<>(communityCards);
            allCards.addAll(player.getHoleCards());
            HandResult result = PokerHandEvaluator.evaluateBestHand(allCards);
            if (bestResult == null || result.compareTo(bestResult) > 0) {
                bestResult = result;
                winners.clear();
                winners.add(Pair.newPair(player.getPlayerId(), result));
            } else if (result.compareTo(bestResult) == 0) {
                winners.add(Pair.newPair(player.getPlayerId(), result));
            }
        }

        return winners;
    }

    public static void main(String[] args) {
//        List<Integer> joker = PokerCardUtils.getPokerIntIdExceptJoker();
//        Collections.shuffle(joker);
//        List<Integer> list = joker.subList(0, 5);
//        List<Card> publicCards = list.stream().map(Card::new).collect(Collectors.toList());
//        list.clear();
        //全部相比
        List<Integer> list = new ArrayList<>(13);
        for (int i = 1; i < 14; i++) {
            list.add(i);
        }
        List<Card> publicCards = List.of(
                new Card(1, list.remove(2)), // ♠A
                new Card(2, list.remove(2)), // ♠K
                new Card(3, list.remove(2)), // ♠Q
                new Card(1, list.remove(2)), // ♠J
                new Card(2, list.remove(2))  // ♠10
        );
        List<Card> cards = new ArrayList<>();

        //非阶级相比
        System.out.println("公牌" + publicCards);
        //随机生成5张
        //随机生成 9 2
        List<PlayerHand> players = new ArrayList<>();
        cards.add(new Card(1, list.remove(0)));
        cards.add(new Card(2, list.remove(0)));
        players.add(new PlayerHand(1, cards));
        Collections.shuffle(list);
        for (int i = 1; i < 9; i++) {
            if (list.size() < 4) {
                break;
            }
            cards = new ArrayList<>();
            cards.add(new Card(1, list.remove(0)));
            cards.add(new Card(2, list.remove(0)));
            players.add(new PlayerHand(i + 1, cards));
        }
        List<Pair<Long, HandResult>> winners = findWinners(players, publicCards);
        System.out.println("赢家是: " + winners);
    }
}


