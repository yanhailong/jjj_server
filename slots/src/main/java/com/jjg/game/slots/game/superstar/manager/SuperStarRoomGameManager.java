package com.jjg.game.slots.game.superstar.manager;

import com.jjg.game.core.data.RoomType;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 超级明星游戏管理器
 */
@Component
public class SuperStarRoomGameManager extends AbstractSuperStarGameManager {
    public SuperStarRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
