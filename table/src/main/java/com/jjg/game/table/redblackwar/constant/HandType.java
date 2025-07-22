package com.jjg.game.table.redblackwar.constant;

/**
 * @author lm
 * @date 2025/7/17 11:54
 */

import java.util.HashMap;
import java.util.Map;

/**
 * 牌型
 */
public enum HandType {
    /**
     * 散牌
     */
    HIGH_CARD(1),
    /**
     * 对子
     */
    PAIR(2),
    /**
     * 顺子
     */
    STRAIGHT(3),
    /**
     * 金花
     */
    FLUSH(4),
    /**
     * 顺金
     */
    STRAIGHT_FLUSH(5),
    /**
     * 豹子
     */
    LEOPARD(6);
    private final int rank;
    HandType(int rank) {
        this.rank = rank;
    }

    private final static Map<Integer, HandType> map = new HashMap<>();

    static {
        for (HandType handType : HandType.values()) {
            map.put(handType.rank, handType);
        }
    }

    public static HandType getHandType(int rank) {
        return map.get(rank);
    }
    public int getRank() {
        return rank;
    }

    public int comper(HandType handType){
        return this.rank - handType.rank;
    }
}