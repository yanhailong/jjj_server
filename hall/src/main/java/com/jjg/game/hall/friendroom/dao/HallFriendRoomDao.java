package com.jjg.game.hall.friendroom.dao;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.room.AbstractFriendRoomDao;
import com.jjg.game.core.data.*;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Repository;

/**
 * 大厅查询时使用
 *
 * @author 2CL
 */
@Repository
public class HallFriendRoomDao extends AbstractFriendRoomDao<FriendRoom, RoomPlayer> {

    public HallFriendRoomDao() {
        super(FriendRoom.class, RoomPlayer.class);
    }

    public HallFriendRoomDao(Class<FriendRoom> roomClazz, Class<RoomPlayer> roomPlayerClazz) {
        super(roomClazz, roomPlayerClazz);
    }

    @Override
    protected FriendRoom createNewEmptyFriendRoom(WarehouseCfg warehouseCfg) {
        RoomType roomType = RoomType.getRoomType(warehouseCfg.getId());
        switch (roomType) {
            case BET_ROOM, BET_TEAM_UP_ROOM -> {
                return new BetFriendRoom();
            }
            case POKER_ROOM, POKER_TEAM_UP_ROOM -> {
                return new PokerFriendRoom();
            }
        }
        return null;
    }
}
