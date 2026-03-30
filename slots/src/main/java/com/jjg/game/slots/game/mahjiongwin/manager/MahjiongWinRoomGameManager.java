package com.jjg.game.slots.game.mahjiongwin.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MahjiongWinRoomGameManager extends AbstractMahjiongWinGameManager{
    public MahjiongWinRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(MahjiongWinResultLib resultLib, MahjiongWinPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<MahjiongWinPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return MahjiongWinPlayerGameDataRoomDTO.class;
    }
}
