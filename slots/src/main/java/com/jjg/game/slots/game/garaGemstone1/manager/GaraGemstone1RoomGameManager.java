package com.jjg.game.slots.game.garaGemstone1.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1PlayerGameData;
import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1ResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GaraGemstone1RoomGameManager extends AbstractGaraGemstone1GameManager {
    public GaraGemstone1RoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(GaraGemstone1ResultLib resultLib, GaraGemstone1PlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
