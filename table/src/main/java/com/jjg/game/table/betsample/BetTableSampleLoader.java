package com.jjg.game.table.betsample;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.function.IntFunction;

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
        Set<EGameType> eGameTypes = EGameType.getGameTypesSetByRoomType(RoomType.BET_ROOM);
        return (Integer[]) eGameTypes.stream().map(EGameType::getGameTypeId).toArray((IntFunction<Object[]>) value -> new Integer[]{});
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
