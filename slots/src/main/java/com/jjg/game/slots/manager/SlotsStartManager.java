package com.jjg.game.slots.manager;

import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author 11
 * @date 2025/6/10 16:37
 */
@Component
public class SlotsStartManager implements IRoomStartListener {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsSampleManager slotsSampleManager;

    @Override
    public int[] getGameTypes() {
        return SlotsConst.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void start() {
        log.info("正在启动slots游戏...");

        this.slotsSampleManager.init();
//        this.dollarExpressManager.init();
    }

    @Override
    public void shutdown() {
        log.info("正在关闭slots游戏...");
    }
}
