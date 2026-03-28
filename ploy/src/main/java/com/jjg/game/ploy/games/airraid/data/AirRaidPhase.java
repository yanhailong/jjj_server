package com.jjg.game.ploy.games.airraid.data;

/**
 * @author 11
 * @date 2026/3/27
 */
public enum AirRaidPhase {
    BETTING(0),    // 下注阶段
    BETTING_END_BET(1),    // 停止下注
    FLYING(2),     // 飞行阶段
    CRASHED(3);    // 坠毁结算阶段

    private final int code;

    AirRaidPhase(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AirRaidPhase fromCode(int code) {
        for (AirRaidPhase phase : values()) {
            if (phase.code == code) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown AirRaidPhase code: " + code);
    }
}
