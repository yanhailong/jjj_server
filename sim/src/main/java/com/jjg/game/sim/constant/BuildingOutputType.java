package com.jjg.game.sim.constant;

/**
 * 建筑产出
 *
 * @author 11
 * @date 2026/6/5
 */
public enum BuildingOutputType {
    //金币
    GOLD(1),
    //能量
    POWER(2),
    //服务能力
    SERVICE(3),
    //游戏上限
    GAME_LIMIT(4),
    //知名度
    AWARENESS(5),
    //曝光度
    EXPOSURE(6),
    //联盟奖杯
    LEAGUE_TROPHY(7),
    //宣传牌
    SIGN(8),
    //风云榜
    TOP_RANK(9),
    //装饰
    DECORATION(10),
    //管理属性(它是一个集合体，包括了SERVICE，AWARENESS，EXPOSURE)
    MANAGE_ARRT(11)
    ;

    private final int code;

    BuildingOutputType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BuildingOutputType fromCode(int code) {
        switch (code) {
            case 1 -> {
                return GOLD;
            }
            case 2 -> {
                return POWER;
            }
            case 3 -> {
                return SERVICE;
            }
            case 4 -> {
                return GAME_LIMIT;
            }
            case 5 -> {
                return AWARENESS;
            }
            case 6 -> {
                return EXPOSURE;
            }
            case 7 -> {
                return LEAGUE_TROPHY;
            }
            case 8 -> {
                return SIGN;
            }
            case 9 -> {
                return TOP_RANK;
            }
            case 10 -> {
                return DECORATION;
            }
            case 11 -> {
                return MANAGE_ARRT;
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * 加成归类: 管理区三属性(SERVICE/AWARENESS/EXPOSURE)统一归入集合体 MANAGE_ARRT, 其余为自身。
     * 用于按产出类型查找对应的加成值。
     */
    public BuildingOutputType bonusGroup() {
        return switch (this) {
            case SERVICE, AWARENESS, EXPOSURE -> MANAGE_ARRT;
            default -> this;
        };
    }
}
