package com.jjg.game.ploy.games.luckypoker.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2026/3/20
 */
public enum PokerRank {
    //散牌
    HIGH_CARD(0),
    //一对J或者更大
    ONE_PAIR_OR_BETTER(1),
    //两对
    TWO_PAIR(2),
    //三张
    THREE_OF_A_KIND(3),
    //顺子
    STRAIGHT(4),
    //同花
    FLUSH(5),
    //葫芦
    FULL_HOUSE(6),
    //四条
    FOUR_OF_A_KIND(7),
    //同花顺
    STRAIGHT_FLUSH(8),
    //皇家同花顺
    ROYAL_FLUSH(9);

    public final int rank;

    PokerRank(int rank) {
        this.rank = rank;
    }

    private static final Map<Integer, PokerRank> map = new HashMap<>();

    public static PokerRank rankOf(int rank) {
        if (map.isEmpty()) {
            for (PokerRank pr : PokerRank.values()) {
                map.put(pr.rank, pr);
            }
        }
        return map.get(rank);
    }
}
