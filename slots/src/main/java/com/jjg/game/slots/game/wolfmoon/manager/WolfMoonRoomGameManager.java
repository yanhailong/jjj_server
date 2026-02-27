package com.jjg.game.slots.game.wolfmoon.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonPlayerGameData;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class WolfMoonRoomGameManager extends AbstractWolfMoonGameManager {
    public WolfMoonRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(WolfMoonResultLib resultLib, WolfMoonPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<WolfMoonPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return WolfMoonPlayerGameDataRoomDTO.class;
    }
}
