package com.jjg.game.slots.game.wealthbank.manager;

import com.jjg.game.core.data.RoomType;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WealthBankRoomGameManager extends AbstractWealthBankGameManager {
    public WealthBankRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
