package com.jjg.game.poker.game.texas.util;

/**
 * @author lm
 * @date 2025/7/30 15:32
 */
public enum HandRank {
    //散排
    HIGH_CARD(0),
    //一对
    ONE_PAIR(1),
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

    HandRank(int rank) {
        this.rank = rank;
    }
}