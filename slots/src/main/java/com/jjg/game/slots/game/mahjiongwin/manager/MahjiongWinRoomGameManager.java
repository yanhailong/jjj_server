package com.jjg.game.slots.game.mahjiongwin.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MahjiongWinRoomGameManager extends AbstractMahjiongWinGameManager{
    public MahjiongWinRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    protected PoolCfg randWinPool(MahjiongWinPlayerGameData playerGameData, int poolId) {
        return null;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
