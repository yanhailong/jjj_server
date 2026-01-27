package com.jjg.game.poker.game.tosouth.manager;

import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToSouthStartManager implements IRoomStartListener, GmListener {
    private static final Logger log = LoggerFactory.getLogger(ToSouthStartManager.class);

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        return null;
    }

    @Override
    public void start() {
        log.info("正在启动南方前进游戏...");
    }

    @Override
    public void shutdown() {
        log.info("正在关闭南方前进游戏...");
    }
}
