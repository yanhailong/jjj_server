package com.jjg.game.table.loongtigerwar.manager;

import com.jjg.game.table.loongtigerwar.constant.LoongTigerWarConstant;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/27 17:53
 */
@Component
public class LoongTigerWarStartManager implements IRoomStartListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    LoongTigerWarSampleManager sampleManager;

    @Override
    public void start() {
        log.info("正在启动龙虎斗游戏...");
        sampleManager.initSampleConfig();
    }

    @Override
    public void shutdown() {
        log.info("正在关闭龙虎斗游戏...");
    }
}
