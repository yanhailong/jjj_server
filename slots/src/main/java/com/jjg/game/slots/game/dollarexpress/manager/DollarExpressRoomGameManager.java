package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DollarExpressRoomGameManager extends AbstractDollarExpressGameManager {

    public DollarExpressRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(DollarExpressResultLib resultLib, DollarExpressPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<DollarExpressPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return DollarExpressPlayerGameDataRoomDTO.class;
    }
}
