package com.jjg.game.slots.game.garaGemstone2.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.garaGemstone2.data.GaraGemstone2PlayerGameData;
import com.jjg.game.slots.game.garaGemstone2.data.GaraGemstone2PlayerGameDataRoomDTO;
import com.jjg.game.slots.game.garaGemstone2.data.GaraGemstone2ResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GaraGemstone2RoomGameManager extends AbstractGaraGemstone2GameManager {
    public GaraGemstone2RoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(GaraGemstone2ResultLib resultLib, GaraGemstone2PlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return GaraGemstone2PlayerGameDataRoomDTO.class;
    }
}
