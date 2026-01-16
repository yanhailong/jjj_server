package com.jjg.game.slots.game.captainjack.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackGameDataDao;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CaptainJackRoomGameManager extends AbstractCaptainJackGameManager{
    public CaptainJackRoomGameManager(CaptainJackGameGenerateManager gameGenerateManager,
                                  CaptainJackGameDataDao gameDataDao, CaptainJackResultLibDao captainJackResultLibDao) {
        super(gameGenerateManager, gameDataDao, captainJackResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
