package com.jjg.game.slots.game.candyparty.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.candyparty.dao.CandyPartyResultLibDao;
import com.jjg.game.slots.game.candyparty.data.CandyPartyPlayerGameData;
import com.jjg.game.slots.game.candyparty.data.CandyPartyResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CandyPartyRoomGameManager extends AbstractCandyPartyGameManager {
    public CandyPartyRoomGameManager(CandyPartyGameGenerateManager gameGenerateManager, CandyPartyResultLibDao candyPartyResultLibDao) {
        super(gameGenerateManager,candyPartyResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(CandyPartyResultLib resultLib, CandyPartyPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

}
