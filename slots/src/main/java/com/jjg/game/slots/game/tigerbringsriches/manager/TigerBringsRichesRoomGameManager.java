package com.jjg.game.slots.game.tigerbringsriches.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesGameDataDao;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesResultLibDao;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesPlayerGameData;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/18 15:02
 */
@Component
public class TigerBringsRichesRoomGameManager extends AbstractTigerBringsRichesGameManager {
    public TigerBringsRichesRoomGameManager(TigerBringsRichesGameGenerateManager gameGenerateManager, TigerBringsRichesGameDataDao gameDataDao, TigerBringsRichesResultLibDao TigerBringsRichesResultLibDao) {
        super(gameGenerateManager, gameDataDao, TigerBringsRichesResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(TigerBringsRichesResultLib resultLib, TigerBringsRichesPlayerGameData playerGameData) {
        return List.of();
    }
}
