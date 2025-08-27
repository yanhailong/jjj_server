package com.jjg.game.poker.game.common.dao;

import com.jjg.game.core.dao.room.AbstractFriendRoomDao;
import com.jjg.game.core.data.PokerFriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Repository;

/**
 * 扑克好友房dao
 *
 * @author 2CL
 */
@Repository
public class PokerFriendRoomDao extends AbstractFriendRoomDao<PokerFriendRoom, RoomPlayer> {

    public PokerFriendRoomDao(Class<PokerFriendRoom> roomClazz, Class<RoomPlayer> roomPlayerClazz) {
        super(roomClazz, roomPlayerClazz);
    }

    @Override
    protected PokerFriendRoom createNewEmptyFriendRoom(WarehouseCfg warehouseCfg) {
        return new PokerFriendRoom();
    }
}
