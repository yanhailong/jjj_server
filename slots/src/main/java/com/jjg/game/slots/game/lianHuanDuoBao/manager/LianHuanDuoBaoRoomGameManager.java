package com.jjg.game.slots.game.lianHuanDuoBao.manager;

import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.lianHuanDuoBao.dao.LianHuanDuoBaoResultLibDao;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoPlayerGameData;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoResultLib;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author lm
 * @date 2026/6/2
 */
//TODO 连环夺宝配表完成后取消注释即可启用
//@Component
public class LianHuanDuoBaoRoomGameManager extends AbstractLianHuanDuoBaoGameManager {
    public LianHuanDuoBaoRoomGameManager(LianHuanDuoBaoGameGenerateManager gameGenerateManager,
                                         LianHuanDuoBaoResultLibDao resultLibDao) {
        super(gameGenerateManager, resultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }

    @Override
    protected List<Integer> checkLibPool(LianHuanDuoBaoResultLib resultLib, LianHuanDuoBaoPlayerGameData playerGameData) {
        return Collections.emptyList();
    }
}
