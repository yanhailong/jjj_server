package com.jjg.game.poker.game.texas.manager;

import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author 11
 * @date 2025/6/27 17:53
 */
@Component
public class TexasStartManager implements IRoomStartListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TexasSampleManager texasSampleManager;

    @Override
    public void start() {
        log.info("正在启动德州游戏...");

        texasSampleManager.init();
    }

    @Override
    public void shutdown() {
        log.info("正在关闭德州游戏...");
    }
}
