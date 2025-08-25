package com.jjg.game.hall.dao;

import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.data.*;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/6/25 17:05
 */
@Repository
public class HallRoomDao extends AbstractRoomDao<Room, RoomPlayer> {

    public HallRoomDao() {
        super(Room.class, RoomPlayer.class);
    }
}
