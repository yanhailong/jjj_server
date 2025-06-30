package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConst;
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
public class DollarExpressStartManager implements IRoomStartListener {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressManager dollarExpressManager;
    @Autowired
    private DollarRoomManager roomManager;
    @Autowired
    private DollarExpressSampleManager dollarExpressSampleManager;

    @Override
    public int[] getGameTypes() {
        return DollarExpressConst.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void start() {
        log.info("正在启动美元快递游戏...");

        this.dollarExpressSampleManager.init();
        this.dollarExpressManager.init();

        roomManager.test();
    }


    @Override
    public void shutdown() {
        log.info("正在关闭美元快递游戏...");
    }
}
