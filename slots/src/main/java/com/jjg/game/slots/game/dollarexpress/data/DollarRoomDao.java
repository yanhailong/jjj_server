package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.core.dao.AbstractRoomDao;
import com.jjg.game.core.data.RoomPlayer;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/25 15:47
 */
@Component
public class DollarRoomDao extends AbstractRoomDao<DollarRoom, RoomPlayer> {

    public DollarRoomDao() {
        super(DollarRoom.class,RoomPlayer.class);
    }
}
