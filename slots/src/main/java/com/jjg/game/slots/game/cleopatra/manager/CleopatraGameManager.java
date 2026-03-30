package com.jjg.game.slots.game.cleopatra.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 麻将胡了游戏逻辑处理器
 *
 * @author 11
 * @date 2025/8/1 17:25
 */
@Component
public class CleopatraGameManager extends AbstractCleopatraGameManager {

    public CleopatraGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
