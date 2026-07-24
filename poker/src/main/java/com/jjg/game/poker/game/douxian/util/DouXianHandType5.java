package com.jjg.game.poker.game.douxian.util;

/**
 * 仙界（史诗阶，5张牌）牌型，见 DESIGN.md 4.5。规则与标准德州牌型一一对应，
 * 复用 {@link com.jjg.game.poker.game.texas.util.PokerHandEvaluator} 的分类逻辑，
 * 仅牌型倍率/牌型值与命名不同（无皇家同花顺特例）。
 */
public enum DouXianHandType5 implements IDouXianHandType {
    //同花顺：5张花色相同且点数连续
    WU_ZHUA_JIN_LONG(1012, "五爪金龙", 6, 360),
    //四条
    SI_XIANG_SHEN_GONG(1013, "四象神功", 5, 280),
    //葫芦：3张相同+2张相同
    SAN_QING_LIANG_YI(1014, "三清两仪", 5, 200),
    //顺子
    JIAN_GUAN_CHANG_KONG(1015, "剑贯长空", 2, 100),
    //同花
    TIAN_HUA_LUAN_ZHUI(1016, "天花乱坠", 2, 70),
    //三条
    SAN_QING_JUE(1017, "三清诀", 1, 45),
    //两对
    QIAN_KUN_DUI(1018, "乾坤对", 1, 30),
    //一对
    LIANG_YI(1019, "两仪", 1, 15),
    //高牌
    SAN_SHOU(1020, "散手", 1, 0);

    private final int configId;
    private final String displayName;
    private final int multiplier;
    private final int value;

    DouXianHandType5(int configId, String displayName, int multiplier, int value) {
        this.configId = configId;
        this.displayName = displayName;
        this.multiplier = multiplier;
        this.value = value;
    }

    @Override
    public int getConfigId() {
        return configId;
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
