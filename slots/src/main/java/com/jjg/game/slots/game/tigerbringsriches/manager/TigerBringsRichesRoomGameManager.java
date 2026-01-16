package com.jjg.game.slots.game.tigerbringsriches.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesGameDataDao;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesResultLibDao;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/18 15:02
 */
@Component
public class TigerBringsRichesRoomGameManager extends AbstractTigerBringsRichesGameManager {
    public TigerBringsRichesRoomGameManager(TigerBringsRichesGameGenerateManager gameGenerateManager, TigerBringsRichesGameDataDao gameDataDao, TigerBringsRichesResultLibDao TigerBringsRichesResultLibDao) {
        super(gameGenerateManager, gameDataDao, TigerBringsRichesResultLibDao);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
