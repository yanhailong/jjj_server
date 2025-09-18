package com.jjg.game.core.constant;

/**
 * @author lm
 * @date 2025/9/17 19:53
 */
public enum RechargeType {
    /**
     * 等级礼包
     */
    PLAYER_LEVEL_GIFT(1);
    private final int type;

    RechargeType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
