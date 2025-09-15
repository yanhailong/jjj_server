package com.jjg.game.common.constant;

/**
 * 功能类型
 *
 * @author 2CL
 */
public enum EFunctionType {
    // 好友房
    FRIEND_ROOM(1),
    // 模拟经营
    SIMULATE_MANAGEMENT(2);

    // 功能ID
    private final int functionId;

    EFunctionType(int functionId) {
        this.functionId = functionId;
    }

    public int getFunctionId() {
        return functionId;
    }
}
