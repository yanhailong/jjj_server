package com.jjg.game.slots.game.tenfoldgoldenbull.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullResultLibDao;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullPlayerGameData;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/18 15:02
 */
@Component
public class TenFoldGoldenBullRoomGameManager extends AbstractTenFoldGoldenBullGameManager {
    public TenFoldGoldenBullRoomGameManager(TenFoldGoldenBullGameGenerateManager gameGenerateManager, TenFoldGoldenBullResultLibDao TenFoldGoldenBullResultLibDao) {
        super(gameGenerateManager, TenFoldGoldenBullResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(TenFoldGoldenBullResultLib resultLib, TenFoldGoldenBullPlayerGameData playerGameData) {
        return List.of();
    }

}
