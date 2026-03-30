package com.jjg.game.slots.game.wealthgod.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.wealthgod.data.WealthGodPlayerGameData;
import com.jjg.game.slots.game.wealthgod.data.WealthGodPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.wealthgod.data.WealthGodResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class WealthGodRoomGameManager extends AbstractWealthGodGameManager{
    public WealthGodRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(WealthGodResultLib resultLib, WealthGodPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<WealthGodPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return WealthGodPlayerGameDataRoomDTO.class;
    }
}
