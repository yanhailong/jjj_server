package com.jjg.game.slots.game.mahjiongwin2.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2PlayerGameData;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2ResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MahjiongWin2RoomGameManager extends AbstractMahjiongWin2GameManager {
    public MahjiongWin2RoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(MahjiongWin2ResultLib resultLib, MahjiongWin2PlayerGameData playerGameData) {
        return Collections.emptyList();
    }

}
