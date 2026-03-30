package com.jjg.game.slots.game.hotfootball.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.hotfootball.data.HotFootballPlayerGameData;
import com.jjg.game.slots.game.hotfootball.data.HotFootballPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.hotfootball.data.HotFootballResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class HotFootballRoomGameManager extends AbstractHotFootballGameManager{
    public HotFootballRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(HotFootballResultLib resultLib, HotFootballPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<HotFootballPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return HotFootballPlayerGameDataRoomDTO.class;
    }
}
