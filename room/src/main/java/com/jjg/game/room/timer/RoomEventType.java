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
    ROOM_TICK("房间tick"),
    ROBOT_BET_LOOP("机器人循环押注"),
    ROOM_EMPTY_ROOM_CHECK("房间空房间检测"),
    ;
    final String eventTypeName;

    RoomEventType(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public String getEventTypeName() {
        return eventTypeName;
    }
}
