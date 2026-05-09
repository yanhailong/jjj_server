package com.jjg.game.slots.game.christmasBashNight.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightPlayerGameData;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ChristmasBashNightRoomGameManager extends AbstractChristmasBashNightGameManager {
    public ChristmasBashNightRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(ChristmasBashNightResultLib resultLib, ChristmasBashNightPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
