package com.jjg.game.slots.game.pegasusunbridle.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleGameDataDao;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleResultLibDao;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridlePlayerGameData;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/18 15:02
 */
@Component
public class PegasusUnbridleRoomGameManager extends AbstractPegasusUnbridleGameManager{
    public PegasusUnbridleRoomGameManager(PegasusUnbridleGameGenerateManager gameGenerateManager, PegasusUnbridleGameDataDao gameDataDao, PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao) {
        super(gameGenerateManager, gameDataDao, PegasusUnbridleResultLibDao);
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(PegasusUnbridleResultLib resultLib, PegasusUnbridlePlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
