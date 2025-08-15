package com.jjg.game.table.common.dao;

import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.dao.AbstractGoldRoomDao;
import org.springframework.stereotype.Component;

/**
 * Table房间Dao
 *
 * @author 2CL
 */
@Component
public class TableRoomDao extends AbstractGoldRoomDao<BetTableRoom, RoomPlayer> {

    public TableRoomDao() {
        super(BetTableRoom.class, RoomPlayer.class);
    }
}
