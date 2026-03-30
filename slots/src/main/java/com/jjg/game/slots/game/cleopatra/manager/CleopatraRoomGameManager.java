package com.jjg.game.slots.game.cleopatra.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameData;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CleopatraRoomGameManager extends AbstractCleopatraGameManager {
    public CleopatraRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(CleopatraResultLib resultLib, CleopatraPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<CleopatraPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return CleopatraPlayerGameDataRoomDTO.class;
    }
}
