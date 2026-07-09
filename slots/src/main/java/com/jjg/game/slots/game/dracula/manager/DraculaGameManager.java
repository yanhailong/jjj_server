package com.jjg.game.slots.game.dracula.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 德古拉黑暗财富 游戏逻辑处理器
 */
@Component
public class DraculaGameManager extends AbstractDraculaGameManager {
    public DraculaGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
