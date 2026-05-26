package com.jjg.game.sim.constant;

/**
 * 建筑产出类型
 *
 * @author 11
 * @date 2026/5/26
 */
public enum BuildingOutputType {

    /** 金币 / 分钟 */
    COIN(1),
    /** 能量 / 秒 */
    POWER(2),
    /** 繁荣度增益 (前台接待区) */
    PROSPERITY(3),
    /** 知名度增益 (运营部) */
    AWARENESS(4),
    /** 曝光度增益 (营销部) */
    EXPOSURE(5),
    /** 研发解锁上限 (研发部) */
    RESEARCH_CAP(6);

    private final int code;

    BuildingOutputType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static BuildingOutputType of(int code) {
        for (BuildingOutputType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
