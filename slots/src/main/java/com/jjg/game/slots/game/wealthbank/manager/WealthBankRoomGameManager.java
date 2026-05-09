package com.jjg.game.slots.game.wealthbank.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.wealthbank.data.WealthBankPlayerGameData;
import com.jjg.game.slots.game.wealthbank.data.WealthBankResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class WealthBankRoomGameManager extends AbstractWealthBankGameManager {
    public WealthBankRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(WealthBankResultLib resultLib, WealthBankPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
