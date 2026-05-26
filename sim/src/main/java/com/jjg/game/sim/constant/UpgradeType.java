package com.jjg.game.sim.constant;

/**
 * 建筑升级类型
 *
 * @author 11
 * @date 2026/5/26
 */
public enum UpgradeType {

    /** 主等级升级 */
    MAIN_LEVEL(1),
    /** 扩容人数升级 */
    EXPANSION(2);

    private final int code;

    UpgradeType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static UpgradeType of(int code) {
        for (UpgradeType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
