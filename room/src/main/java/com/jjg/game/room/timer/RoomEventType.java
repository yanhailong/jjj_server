package com.jjg.game.room.timer;

/**
 * 房间事件类型
 *
 * @author 2CL
 */
public enum RoomEventType {
    // 房间阶段运行事件
    ROOM_PHASE_RUN_EVENT("房间阶段运行事件"),
    TRIGGER_ROBOT_BET_ACTION("触发机器人押注"),
    ROBOT_BET_LOOP("机器人循环押注"),
    ROOM_EMPTY_ROOM_CHECK("游戏空房间检测"),
    POKER_PLAYER_EVENT("扑克玩家事件，玩家操作倒计时"),
    ROOM_SAVE_PLAYER_DATA("房间玩家数据回存"),
    ROOM_TICK("房间tick"),
    CURRENCY_CHANGE_EVENT("货币变化事件"),
    PLAYER_RECHARGE_EVENT("玩家充值事件"),
    ;
    final String eventTypeName;

    RoomEventType(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public String getEventTypeName() {
        return eventTypeName;
    }
}
