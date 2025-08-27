package com.jjg.game.room.base;

/**
 * 房间状态,记录房间各个时间节点的状态,方便观察日志记录
 *
 * @author 2CL
 */
public enum ERoomState {
    // 初始化完成
    INIT_START("初始化开始"),
    // 房间就绪，对于需要等房间人满才开始的房间，此状态在人满之后设置
    READY("房间就绪"),
    // 游戏中
    GAMING("游戏中"),
    // 暂停中
    PAUSING("暂停中"),
    // 游戏暂停中
    PAUSED("游戏已暂停"),
    // 房间结算
    ROOM_SETTLEMENT("房间大结算"),
    // 房间结算完后完全结束,包括大循环
    ROOM_FINISHED("房间完全结束"),
    // 房间shutdown时
    ROOM_DESTROYING("房间销毁中"),
    // 房间shutdown结束
    ROOM_DESTROYED("房间销毁结束"),
    ;

    /**
     * 状态描述
     */
    private final String stateDesc;

    /**
     * 当前状态可以进行到下一个状态的合集
     */
    private ERoomState[] nextState;

    ERoomState(String stateDesc) {
        this.stateDesc = stateDesc;
    }

    public String getStateDesc() {
        return stateDesc;
    }
}
