package com.jjg.game.slots.game.thor.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.thor.data.ThorPlayerGameData;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ThorRoomGameManager extends AbstractThorGameManager{
    public ThorRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    protected PoolCfg randWinPool(ThorPlayerGameData playerGameData, int poolId) {
        return null;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
