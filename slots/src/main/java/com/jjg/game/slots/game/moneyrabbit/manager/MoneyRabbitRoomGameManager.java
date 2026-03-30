package com.jjg.game.slots.game.moneyrabbit.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitPlayerGameData;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MoneyRabbitRoomGameManager extends AbstractMoneyRabbitGameManager {
    public MoneyRabbitRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(MoneyRabbitResultLib resultLib, MoneyRabbitPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<MoneyRabbitPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return MoneyRabbitPlayerGameDataRoomDTO.class;
    }
}
