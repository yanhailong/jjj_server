package com.jjg.game.poker.game.tosouth.util;

/**
 * 南方前进牌型
 */
public enum ToSouthCardType {
    NONE(0), //错误牌型
    SINGLE(1), // 单张
    PAIR(2), // 对子
    TRIPLE(3), // 三张
    STRAIGHT(4), // 顺子
    CONSECUTIVE_PAIRS(5), // 连对 (特殊牌型)
    BOMB_QUAD(6), // 炸弹 (四张)
    INSTANT_WIN(7); // 通杀

    private final int type;

    ToSouthCardType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}