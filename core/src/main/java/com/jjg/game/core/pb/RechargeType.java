package com.jjg.game.core.pb;

/**
 * @author lm
 * @date 2025/9/17 19:53
 */
public enum RechargeType {
    /**
     * 商城
     */
    SHOP(1),

    /**
     * 等级礼包
     */
    PLAYER_LEVEL_GIFT(2),

    /**
     * 首充
     */
    FIRST_PAYMENT(3),

    /**
     * 储钱罐
     */
    PIGGY_BANK(4),

    /**
     * 刮刮乐
     */
    SCRATCH_CARDS(5),

    /**
     * 特权卡
     */
    PRIVILEGE_CARD(6),

    /**
     * 成长基金
     */
    GROWTH_FUND(7),
    /**
     * 每日充值
     */
    DAILY_RECHARGE(8),
    /**
     * 后台充值
     */
    BACKEND(9),
    ;
    private final int type;

    RechargeType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static RechargeType valueOf(int type) {
        for (RechargeType rt : RechargeType.values()) {
            if (rt.type == type) {
                return rt;
            }
        }
        return null;
    }
}
