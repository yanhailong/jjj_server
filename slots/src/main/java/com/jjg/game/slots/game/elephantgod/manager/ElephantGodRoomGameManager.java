package com.jjg.game.slots.game.elephantgod.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.elephantgod.dao.ElephantGodGameDataDao;
import com.jjg.game.slots.game.elephantgod.dao.ElephantGodResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElephantGodRoomGameManager extends AbstractElephantGodGameManager{

    public ElephantGodRoomGameManager(ElephantGodResultLibDao libDao, ElephantGodGenerateManager generateManager, ElephantGodGameDataDao gameDataDao) {
        super(libDao, generateManager, gameDataDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
