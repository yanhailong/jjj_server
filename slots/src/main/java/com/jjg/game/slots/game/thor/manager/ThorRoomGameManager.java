package com.jjg.game.slots.game.thor.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.thor.data.ThorPlayerGameData;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ThorRoomGameManager extends AbstractThorGameManager{
    public ThorRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(ThorResultLib resultLib, ThorPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
