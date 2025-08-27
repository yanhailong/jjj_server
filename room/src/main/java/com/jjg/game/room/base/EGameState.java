package com.jjg.game.room.base;

/**
 * 游戏状态枚举
 *
 * @author 2CL
 */
public enum EGameState {
    // 初始化完成
    INIT_START("游戏初始化开始"),
    // 初始化完成
    INIT_DONE("游戏初始化完成"),
    // 房间就绪，对于需要等房间人满才开始的房间，此状态在人满之后设置
    READY("游戏就绪"),
    // 游戏中
    GAMING("游戏中"),
    // 暂停中
    PAUSING("暂停中"),
    // 游戏暂停中
    PAUSED("游戏已暂停"),
    // 游戏小结算
    ROUND_SETTLEMENT("房间小结算"),
    // 游戏小循环结束
    ROUND_OVER("房间小循环结束"),
    ;

    /**
     * 状态描述
     */
    private final String stateDesc;

    /**
     * 当前状态可以进行到下一个状态的合集
     */
    private EGameState[] nextState;

    EGameState(String stateDesc) {
        this.stateDesc = stateDesc;
    }

    public String getStateDesc() {
        return stateDesc;
    }
}
