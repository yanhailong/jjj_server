package com.jjg.game.room.base;

/**
 * 房间状态,记录房间各个时间节点的状态,方便观察日志记录
 *
 * @author 2CL
 */
public enum EGameRoomState {
    // 初始化完成
    INIT_DONE("初始化完成"),
    // 等待加入
    WAIT_JOIN("等待加入"),
    // 房间就绪，对于需要等房间人满才开始的房间，此状态在人满之后设置
    READY("房间就绪"),
    // 游戏中
    GAMING("游戏中"),
    // 房间小结算
    ROUND_SETTLEMENT("房间小结算"),
    // 房间小循环结束
    ROUND_OVER("房间小循环结束"),
    // 房间结算
    ROOM_SETTLEMENT("房间大结算"),
    // 房间完全结束,包括大循环
    ROOM_FINISHED("房间完全结束"),
    ;

    /**
     * 状态描述
     */
    private final String stateDesc;

    /**
     * 当前状态可以进行到下一个状态的合集
     */
    private EGameRoomState[] nextState;

    EGameRoomState(String stateDesc) {
        this.stateDesc = stateDesc;
    }

    public String getStateDesc() {
        return stateDesc;
    }
}
