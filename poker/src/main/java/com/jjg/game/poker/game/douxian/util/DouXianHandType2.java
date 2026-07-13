package com.jjg.game.poker.game.douxian.util;

/**
 * 凡界（英雄阶，2张牌）牌型，见 DESIGN.md 4.3
 */
public enum DouXianHandType2 implements IDouXianHandType {
    //同花顺-2：花色相同且点数连续
    QING_LONG("青龙", 1, 70),
    //对子：点数相同
    LIANG_YI("两仪", 1, 50),
    //顺子-2：点数连续
    FEI_JIAN("飞剑", 1, 30),
    //同花-2：花色相同
    TONG_HUA("同花", 1, 15),
    //高牌
    SAN_SHOU("散手", 1, 0);

    private final String displayName;
    private final int multiplier;
    private final int value;

    DouXianHandType2(String displayName, int multiplier, int value) {
        this.displayName = displayName;
        this.multiplier = multiplier;
        this.value = value;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getMultiplier() {
        return multiplier;
    }

    @Override
    public int getValue() {
        return value;
    }
}
