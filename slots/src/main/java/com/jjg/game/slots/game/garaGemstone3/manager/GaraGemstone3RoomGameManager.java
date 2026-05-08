package com.jjg.game.slots.game.garaGemstone3.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3PlayerGameData;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3PlayerGameDataRoomDTO;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3ResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GaraGemstone3RoomGameManager extends AbstractGaraGemstone3GameManager {
    public GaraGemstone3RoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(GaraGemstone3ResultLib resultLib, GaraGemstone3PlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return GaraGemstone3PlayerGameDataRoomDTO.class;
    }
}
