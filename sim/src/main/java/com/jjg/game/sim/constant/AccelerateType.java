package com.jjg.game.sim.constant;

/**
 * CD 加速类型
 *
 * @author 11
 * @date 2026/5/26
 */
public enum AccelerateType {
    //钻石加速
    DIAMOND(1),
    //道具加速
    ITEM(2),
    //广告加速
    AD(3);

    private final int code;

    AccelerateType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static AccelerateType of(int code) {
        for (AccelerateType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
