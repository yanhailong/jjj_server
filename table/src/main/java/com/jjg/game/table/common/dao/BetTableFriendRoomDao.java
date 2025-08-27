package com.jjg.game.table.common.dao;

import com.jjg.game.core.dao.room.AbstractFriendRoomDao;
import com.jjg.game.core.data.BetFriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Repository;

/**
 * 押注类好友房dao
 *
 * @author 2CL
 */
@Repository
public class BetTableFriendRoomDao extends AbstractFriendRoomDao<BetFriendRoom, RoomPlayer> {

    public BetTableFriendRoomDao(Class<BetFriendRoom> roomClazz, Class<RoomPlayer> roomPlayerClazz) {
        super(roomClazz, roomPlayerClazz);
    }

    @Override
    protected BetFriendRoom createNewEmptyFriendRoom(WarehouseCfg warehouseCfg) {
        return new BetFriendRoom();
    }
}
