package com.jjg.game.slots.game.dracula.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.dracula.data.DraculaPlayerGameData;
import com.jjg.game.slots.game.dracula.data.DraculaResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DraculaRoomGameManager extends AbstractDraculaGameManager{
    public DraculaRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(DraculaResultLib resultLib, DraculaPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
