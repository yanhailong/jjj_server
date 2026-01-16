package com.jjg.game.slots.game.pegasusunbridle.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleGameDataDao;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleResultLibDao;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/18 15:02
 */
@Component
public class PegasusUnbridleRoomGameManager extends AbstractPegasusUnbridleGameManager{
    public PegasusUnbridleRoomGameManager(PegasusUnbridleGameGenerateManager gameGenerateManager, PegasusUnbridleGameDataDao gameDataDao, PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao) {
        super(gameGenerateManager, gameDataDao, PegasusUnbridleResultLibDao);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
