package com.jjg.game.sim.constant;

public enum ServerBuildingType {
    //接待区
    WELCOME(1),
    //运营部
    OPERATIONS(2),
    //营销部
    MARKETING(3),
    //休息区
    REST(4),
    ;

    private final int code;

    ServerBuildingType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public int getCode() {
        return code;
    }
}
