package com.jjg.game.sim.constant;

/**
 * 建筑大类
 *
 * @author 11
 * @date 2026/5/26
 */
public enum BuildingCategory {

    /** 游戏类: SLOT / 扑克 / 捕鱼 → 产出金币 */
    GAME(1, true),
    /** 休息类: 普通休息区 / VIP休息区 → 产出能量 */
    REST(2, true),
    /** 前台接待区 → 服务能力(繁荣度) */
    RECEPTION(3, false),
    /** 运营部 → 知名度 */
    OPERATION(4, false),
    /** 营销部 → 曝光度 */
    MARKETING(5, false),
    /** 研发部 → 解锁游戏数量上限 */
    RESEARCH(6, false);

    private final int code;
    /** 是否支持扩容人数升级 */
    private final boolean expandable;

    BuildingCategory(int code, boolean expandable) {
        this.code = code;
        this.expandable = expandable;
    }

    public int code() {
        return code;
    }

    public boolean expandable() {
        return expandable;
    }

    public static BuildingCategory of(int code) {
        for (BuildingCategory c : values()) {
            if (c.code == code) {
                return c;
            }
        }
        return null;
    }
}
