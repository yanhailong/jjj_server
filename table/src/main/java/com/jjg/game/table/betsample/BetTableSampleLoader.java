package com.jjg.game.table.betsample;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 2CL
 */
@Repository
public class BetTableSampleLoader implements IRoomStartListener {

    private static final Logger log = LoggerFactory.getLogger(BetTableSampleLoader.class);
    @Autowired
    private BaseBetTableSampleManager baseBetTableSampleManager;

    @Override
    public Integer[] getGameTypes() {
        List<EGameType> eGameTypes = EGameType.getGameTypesSetByRoomType(RoomType.BET_ROOM);
        Integer[] arr = new Integer[eGameTypes.size()];
        for (int i = 0; i < eGameTypes.size(); i++) {
            arr[i] = eGameTypes.get(i).getGameTypeId();
        }
        return arr;
    }

    @Override
    public void start() {
        try {
            baseBetTableSampleManager.init();
        } catch (Exception exception) {
            log.error("押注类的公共配置表加载异常 {}", exception.getMessage(), exception);
            throw new GameSampleException(exception);
        }
    }

    @Override
    public void shutdown() {

    }
}
