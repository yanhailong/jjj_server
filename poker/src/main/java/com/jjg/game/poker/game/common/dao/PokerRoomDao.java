package com.jjg.game.poker.game.common.dao;

import com.jjg.game.core.data.PokerRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.room.dao.AbstractGoldRoomDao;
import org.springframework.stereotype.Component;

/**
 * Table房间Dao
 *
 * @author 2CL
 */
@Component
public class PokerRoomDao extends AbstractGoldRoomDao<PokerRoom, RoomPlayer> {

    public PokerRoomDao() {
        super(PokerRoom.class, RoomPlayer.class);
    }
}
