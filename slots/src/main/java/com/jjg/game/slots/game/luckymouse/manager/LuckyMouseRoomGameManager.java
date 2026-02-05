package com.jjg.game.slots.game.luckymouse.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.luckymouse.data.LuckyMousePlayerGameData;
import com.jjg.game.slots.game.luckymouse.data.LuckyMousePlayerGameDataRoomDTO;
import com.jjg.game.slots.game.luckymouse.data.LuckyMouseResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class LuckyMouseRoomGameManager extends AbstractLuckyMouseGameManager {
    public LuckyMouseRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(LuckyMouseResultLib resultLib, LuckyMousePlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return LuckyMousePlayerGameDataRoomDTO.class;
    }
}
