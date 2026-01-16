package com.jjg.game.slots.game.basketballSuperstar.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarPlayerGameData;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 篮球巨星游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class BasketballSuperstarRoomGameManager extends AbstractBasketballSuperstarGameManager {
    public BasketballSuperstarRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(BasketballSuperstarResultLib resultLib, BasketballSuperstarPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
