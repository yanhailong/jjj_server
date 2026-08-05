package com.jjg.game.sim.data;

/**
 * 进入游戏的方式
 */
public enum EnterGameType {
    //正常
    NORMAL(0),
    //赛季
    SEASON(1),
    //拜访
    VISIT(2),
    //多人任务
    COOP(3),

    ;

    private final int value;

    EnterGameType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static EnterGameType valueOf(int value) {
        switch (value) {
            case 0 -> {
                return NORMAL;
            }
            case 1 -> {
                return SEASON;
            }
            case 2 -> {
                return VISIT;
            }
            case 3 -> {
                return COOP;
            }
            default -> {
                return NORMAL;
            }
        }
    }
}
