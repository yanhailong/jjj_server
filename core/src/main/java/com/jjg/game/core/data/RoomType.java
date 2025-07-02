package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2025/6/25 9:24
 */
public enum RoomType {
    //主动创建房间，组队
    TEAM(Room.class),
    //金币休闲
    GOLD(GoldRoom.class),
    //押注房间类型
    BET_ROOM(BetTableRoom.class),
    //赛季
    COMPETITION(CompetitionRoom.class),
    //扑克类
    POKER_ROOM(PokerRoom.class);

    private final Class<? extends Room> roomDataType;

    RoomType(Class<? extends Room> roomDataType) {
        this.roomDataType = roomDataType;
    }

    public Class<? extends Room> getRoomDataType() {
        return roomDataType;
    }
}
