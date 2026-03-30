package com.jjg.game.slots.game.steamAge.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.steamAge.data.SteamAgePlayerGameData;
import com.jjg.game.slots.game.steamAge.data.SteamAgePlayerGameDataRoomDTO;
import com.jjg.game.slots.game.steamAge.data.SteamAgeResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 蒸汽时代游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class SteamAgeRoomGameManager extends AbstractSteamAgeGameManager {

    public SteamAgeRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(SteamAgeResultLib resultLib, SteamAgePlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<SteamAgePlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return SteamAgePlayerGameDataRoomDTO.class;
    }
}
