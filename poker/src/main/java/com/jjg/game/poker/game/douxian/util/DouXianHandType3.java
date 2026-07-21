package com.jjg.game.poker.game.douxian.util;

/**
 * 灵界（超凡阶，3张牌）牌型，见 DESIGN.md 4.4
 */
public enum DouXianHandType3 implements IDouXianHandType {
    //同花顺-3：花色相同且点数连续
    ZHI_ZUN_LONG("至尊龙", 2, 100),
    //3条：点数相同
    SAN_QING_JUE("三清诀", 2, 70),
    //顺子-3：点数连续
    LIAN_HUAN_JIAN("连环剑", 1, 45),
    //同花-3：花色相同
    WANG_YOU_HUA("忘忧花", 1, 30),
    //对子（3张中2张点数相同）
    LIANG_YI("两仪", 1, 15),
    //高牌
    SAN_SHOU("散手", 1, 0);

    private final String displayName;
    private final int multiplier;
    private final int value;

    DouXianHandType3(String displayName, int multiplier, int value) {
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
