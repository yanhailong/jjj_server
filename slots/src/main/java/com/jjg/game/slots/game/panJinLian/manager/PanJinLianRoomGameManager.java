package com.jjg.game.slots.game.panJinLian.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianPlayerGameData;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 潘金莲游戏逻辑管理器（房间模式）
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class PanJinLianRoomGameManager extends AbstractPanJinLianGameManager {

    public PanJinLianRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(PanJinLianResultLib resultLib, PanJinLianPlayerGameData playerGameData) {
        return Collections.emptyList();
    }

    @Override
    protected Class<PanJinLianPlayerGameDataRoomDTO> getSlotsPlayerGameDataDTOCla() {
        return PanJinLianPlayerGameDataRoomDTO.class;
    }
}
