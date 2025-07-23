package com.jjg.game.table.betsample;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 2CL
 */
@Repository
@Order(1)
public class BetTableSampleLoader implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(BetTableSampleLoader.class);
    @Autowired
    private BaseBetTableSampleManager baseBetTableSampleManager;
    private boolean isRunning;

    @Override
    public void start() {
        if (isRunning) {
            return;
        }
        try {
            baseBetTableSampleManager.init();
        } catch (Exception exception) {
            log.error("押注类的公共配置表加载异常 {}", exception.getMessage(), exception);
            throw new GameSampleException(exception);
        }
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return 1;
    }
}
