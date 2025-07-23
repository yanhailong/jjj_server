package com.jjg.game.table.redblackwar.manager;

import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.table.redblackwar.constant.RedBlackWarConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author 11
 * @date 2025/6/27 17:53
 */
@Component
public class RedBlackWarStartManager implements IRoomStartListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private RedBlackWarSampleManager redBlackWarSampleManager;

    @Override
    public Integer[] getGameTypes() {
        return RedBlackWarConstant.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void start() {
        log.info("正在启动红黑大战游戏...");

        //redBlackWarSampleManager.init();
    }

    @Override
    public void shutdown() {
        log.info("正在关闭红黑大战游戏...");
    }
}
