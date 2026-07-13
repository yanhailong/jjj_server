package com.jjg.game.poker.game.douxian.manager;

import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 斗仙牌房间启停生命周期监听 + GM指令入口，实际发牌/洗牌逻辑在 {@link com.jjg.game.poker.game.douxian.data.DouXianDataHelper}。
 */
@Component
public class DouXianStartManager implements IRoomStartListener, GmListener {

    private static final Logger log = LoggerFactory.getLogger(DouXianStartManager.class);

    @Override
    public void start() {
        log.info("正在启动斗仙牌游戏...");
    }

    @Override
    public void shutdown() {
        log.info("正在关闭斗仙牌游戏...");
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        return null;
    }
}
