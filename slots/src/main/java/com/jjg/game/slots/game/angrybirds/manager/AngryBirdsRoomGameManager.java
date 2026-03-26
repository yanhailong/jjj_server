package com.jjg.game.slots.game.angrybirds.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsResultLibDao;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsPlayerGameData;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class AngryBirdsRoomGameManager extends AbstractAngryBirdsGameManager {
    public AngryBirdsRoomGameManager(AngryBirdsGenerateManager gameGenerateManager, AngryBirdsResultLibDao angryBirdsResultLibDao) {
        super(gameGenerateManager, angryBirdsResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(AngryBirdsResultLib resultLib, AngryBirdsPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

}
