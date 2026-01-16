package com.jjg.game.slots.game.basketballSuperstar.manager;

import com.jjg.game.core.data.RoomType;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
}
