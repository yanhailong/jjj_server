package com.jjg.game.slots.game.bountyduel.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 赏金大对决普通房间游戏管理器。
 */
@Component
public class BountyDuelGameManager extends AbstractBountyDuelGameManager {
    public BountyDuelGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
