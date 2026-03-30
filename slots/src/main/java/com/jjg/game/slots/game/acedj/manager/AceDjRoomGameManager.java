package com.jjg.game.slots.game.acedj.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.acedj.data.AceDjPlayerGameDataRoomDTO;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AceDjRoomGameManager extends AbstractAceDjGameManager {
    public AceDjRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected Class<AceDjPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return AceDjPlayerGameDataRoomDTO.class;
    }
}
