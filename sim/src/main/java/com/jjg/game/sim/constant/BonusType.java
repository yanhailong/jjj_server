package com.jjg.game.sim.constant;

/**
 * 加成类型
 *
 * @author 11
 * @date 2026/5/29
 */
public enum BonusType {
    //知名度
    AWARENESS(1, false),
    //服务能力
    SERVICE(2, false),
    //游戏区
    GAME_AREA(3, true),
    //管理区
    MANAGE_AREA(4, false),
    //休息区
    REST_AREA(5, false);

    private final int code;
    //true 每分钟触发
    //false 加成上限
    private final boolean min;

    BonusType(int code, boolean min) {
        this.code = code;
        this.min = min;
    }

    public int getCode() {
        return code;
    }

    public boolean isMin() {
        return min;
    }

    public static BonusType fromCode(int code) {
        switch (code) {
            case 1 -> {
                return AWARENESS;
            }
            case 2 -> {
                return SERVICE;
            }
            case 3 -> {
                return GAME_AREA;
            }
            case 4 -> {
                return MANAGE_AREA;
            }
            case 5 -> {
                return REST_AREA;
            }
            default -> {
                return null;
            }
        }
    }

    public static BonusType fromBuildingType(BuildingType buildingType) {
        switch (buildingType) {
            case GAME -> {
                return GAME_AREA;
            }
            case MANAGE -> {
                return MANAGE_AREA;
            }
            case REST -> {
                return REST_AREA;
            }
            default -> {
                return null;
            }
        }
    }
}
