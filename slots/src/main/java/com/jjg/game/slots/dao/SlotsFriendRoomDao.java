package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.room.AbstractFriendRoomDao;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.SlotsFriendRoom;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.springframework.stereotype.Repository;

@Repository
public class SlotsFriendRoomDao extends AbstractFriendRoomDao<SlotsFriendRoom, RoomPlayer> {
    public SlotsFriendRoomDao() {
        super(SlotsFriendRoom.class);
    }

    @Override
    protected SlotsFriendRoom createNewEmptyFriendRoom(WarehouseCfg warehouseCfg) {
        return new SlotsFriendRoom();
    }
}
