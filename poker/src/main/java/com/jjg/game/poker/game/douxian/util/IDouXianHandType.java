package com.jjg.game.poker.game.douxian.util;

/**
 * 各区域牌型的公共契约：灵力值 = (主体点数 × 牌型倍率 + 牌型值) × 回合倍率
 */
public interface IDouXianHandType {

    String getDisplayName();

    int getMultiplier();

    int getValue();
}
