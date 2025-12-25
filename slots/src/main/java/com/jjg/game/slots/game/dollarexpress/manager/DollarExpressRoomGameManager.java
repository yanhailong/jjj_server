package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DollarExpressRoomGameManager extends AbstractDollarExpressGameManager {

    public DollarExpressRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    protected PoolCfg randWinPool(DollarExpressPlayerGameData playerGameData, int poolId) {
        return null;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
