package com.jjg.game.sim.constant;

/**
 * 建筑类型
 *
 * @author 11
 * @date 2026/5/26
 */
public enum BuildingType {
    //游戏区
    GAME(1),
    //休息区
    REST(2),
    //管理区
    MANAGE(3),
    //参观区
    TOUR(4);

    private final int code;

    BuildingType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public int getCode() {
        return code;
    }

    public static BuildingType fromCode(int code) {
        switch (code) {
            case 1 -> {
                return GAME;
            }
            case 2 -> {
                return REST;
            }
            case 3 -> {
                return MANAGE;
            }
            case 4 -> {
                return TOUR;
            }
            default -> {
                return null;
            }
        }
    }
}
