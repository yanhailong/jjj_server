package com.jjg.game.slots.game.bountyduel.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelPlayerGameData;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BountyDuelRoomGameManager extends AbstractBountyDuelGameManager {
    public BountyDuelRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(BountyDuelResultLib resultLib, BountyDuelPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

}
