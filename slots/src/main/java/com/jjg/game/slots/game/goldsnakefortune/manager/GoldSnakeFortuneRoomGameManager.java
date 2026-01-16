package com.jjg.game.slots.game.goldsnakefortune.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortunePlayerGameData;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GoldSnakeFortuneRoomGameManager extends AbstractGoldSnakeFortuneGameManager {
    public GoldSnakeFortuneRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(GoldSnakeFortuneResultLib resultLib, GoldSnakeFortunePlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
