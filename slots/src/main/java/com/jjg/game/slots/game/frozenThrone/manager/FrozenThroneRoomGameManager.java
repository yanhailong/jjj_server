package com.jjg.game.slots.game.frozenThrone.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThronePlayerGameData;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 寒冰王座游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class FrozenThroneRoomGameManager extends AbstractFrozenThroneGameManager {

    public FrozenThroneRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(FrozenThroneResultLib resultLib, FrozenThronePlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
