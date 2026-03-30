package com.jjg.game.slots.game.superstar.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 超级明星游戏管理器
 */
@Component
public class SuperStarGameManager extends AbstractSuperStarGameManager {
    public SuperStarGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }
}
