package com.jjg.game.slots.game.findgoldcity.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.findgoldcity.dao.FindGoldCityResultLibDao;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityPlayerGameData;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/18 15:02
 */
@Component
public class FindGoldCityRoomGameManager extends AbstractFindGoldCityGameManager {
    public FindGoldCityRoomGameManager(FindGoldCityGameGenerateManager gameGenerateManager, FindGoldCityResultLibDao FindGoldCityResultLibDao) {
        super(gameGenerateManager, FindGoldCityResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(FindGoldCityResultLib resultLib, FindGoldCityPlayerGameData playerGameData) {
        return List.of();
    }
}
