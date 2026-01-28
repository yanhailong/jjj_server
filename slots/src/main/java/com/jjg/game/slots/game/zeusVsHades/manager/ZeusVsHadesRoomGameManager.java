package com.jjg.game.slots.game.zeusVsHades.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesPlayerGameData;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 寒冰王座游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class ZeusVsHadesRoomGameManager extends AbstractZeusVsHadesGameManager {

    public ZeusVsHadesRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }


    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(ZeusVsHadesResultLib resultLib, ZeusVsHadesPlayerGameData playerGameData) {
        return List.of();
    }
}
