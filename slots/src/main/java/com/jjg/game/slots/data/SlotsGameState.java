package com.jjg.game.slots.data;

public enum SlotsGameState {
    GAMING("游戏中"),
    // 游戏暂停中
    PAUSED("游戏已暂停"),
    // 开始销毁
    DESTROYING("游戏开始销毁"),
    // 销毁结束
    DESTROYED("游戏销毁完成");

    /**
     * 状态描述
     */
    private final String stateDesc;

    /**
     * 当前状态可以进行到下一个状态的合集
     */
    private SlotsGameState[] nextState;

    SlotsGameState(String stateDesc) {
        this.stateDesc = stateDesc;
    }

    public String getStateDesc() {
        return stateDesc;
    }
}
