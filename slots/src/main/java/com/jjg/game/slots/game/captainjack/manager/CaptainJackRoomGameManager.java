package com.jjg.game.slots.game.captainjack.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackGameDataDao;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackResultLibDao;
import com.jjg.game.slots.game.captainjack.data.CaptainJackPlayerGameData;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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

    @Override
    protected List<Integer> checkLibPool(CaptainJackResultLib resultLib, CaptainJackPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
