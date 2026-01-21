package com.jjg.game.slots.game.demonchild.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.demonchild.dao.DemonChildGameDataDao;
import com.jjg.game.slots.game.demonchild.dao.DemonChildResultLibDao;
import com.jjg.game.slots.game.demonchild.data.DemonChildPlayerGameData;
import com.jjg.game.slots.game.demonchild.data.DemonChildResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DemonChildRoomGameManager extends AbstractDemonChildGameManager {
    public DemonChildRoomGameManager(DemonChildGameGenerateManager gameGenerateManager,
                                     DemonChildGameDataDao gameDataDao, DemonChildResultLibDao demonChildResultLibDao) {
        super(gameGenerateManager, gameDataDao, demonChildResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(DemonChildResultLib resultLib, DemonChildPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
