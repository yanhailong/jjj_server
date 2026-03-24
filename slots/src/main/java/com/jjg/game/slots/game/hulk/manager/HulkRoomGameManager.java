package com.jjg.game.slots.game.hulk.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.hulk.data.HulkPlayerGameData;
import com.jjg.game.slots.game.hulk.data.HulkResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author 11
 * @date 2026/1/15
 */
@Component
public class HulkRoomGameManager extends AbstractHulkGameManager {
    public HulkRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(HulkResultLib resultLib, HulkPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

}
