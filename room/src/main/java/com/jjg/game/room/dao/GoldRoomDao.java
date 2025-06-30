package com.jjg.game.room.dao;

import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/26 16:40
 */
@Component
public class GoldRoomDao extends AbstractGoldRoomDao<Room, RoomPlayer> {
    public GoldRoomDao() {
        super(Room.class,RoomPlayer.class);
    }
}
