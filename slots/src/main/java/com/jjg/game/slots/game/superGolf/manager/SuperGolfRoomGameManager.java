package com.jjg.game.slots.game.superGolf.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.superGolf.data.SuperGolfPlayerGameData;
import com.jjg.game.slots.game.superGolf.data.SuperGolfResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SuperGolfRoomGameManager extends AbstractSuperGolfGameManager {
    public SuperGolfRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(SuperGolfResultLib resultLib, SuperGolfPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
