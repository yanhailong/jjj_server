package com.jjg.game.room.dao;

import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/6/26 13:52
 */
@Repository
public class RoomDao extends AbstractRoomDao<Room, RoomPlayer> {
    public RoomDao() {
        super(Room.class,RoomPlayer.class);
    }
}
