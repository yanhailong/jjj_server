package com.jjg.game.core.data;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;

/**
 * @author 11
 * @date 2025/6/25 9:24
 */
public enum RoomType {
    //slots
    SLOTS(Room.class),
    //押注房间类型
    BET_ROOM(BetTableRoom.class),
    //押注类主动创建房间，组队
    BET_TEAM_UP_ROOM(BetFriendRoom.class),
    //赛季
    COMPETITION(CompetitionRoom.class),
    //扑克类
    POKER_ROOM(PokerRoom.class),
    //扑克类主动创建房间，组队
    POKER_TEAM_UP_ROOM(PokerFriendRoom.class),
    ;

    private final Class<? extends Room> roomDataType;

    RoomType(Class<? extends Room> roomDataType) {
        this.roomDataType = roomDataType;
    }

    public Class<? extends Room> getRoomDataType() {
        return roomDataType;
    }

    /**
     * 通过房间ID获取房间类型
     */
    public static RoomType getRoomType(int roomCfgId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        EGameType eGameType = EGameType.getGameByTypeId(warehouseCfg.getGameID());
        RoomType roomType = null;
        // 普通房间
        if (warehouseCfg.getRoomType() < GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
            roomType = eGameType.getDefualtRoomType();
        } else {
            // 好友房 2: 百人 3 poker
            int gameType = warehouseCfg.getGameType();
            if (gameType == CoreConst.GameMajorType.TABLE) {
                roomType = RoomType.BET_TEAM_UP_ROOM;
            } else if (gameType == CoreConst.GameMajorType.POKER) {
                roomType = RoomType.POKER_TEAM_UP_ROOM;
            }
        }
        return roomType;
    }
}
