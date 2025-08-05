package com.jjg.game.poker.game.blackjack.manager;

import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
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
public class BlackJackStartManager implements IRoomStartListener {
    private Logger log = LoggerFactory.getLogger(getClass());


    @Override
    public void start() {
        log.info("正在启动21点游戏...");

    }

    @Override
    public void shutdown() {
        log.info("正在关闭21点游戏...");
    }


}
