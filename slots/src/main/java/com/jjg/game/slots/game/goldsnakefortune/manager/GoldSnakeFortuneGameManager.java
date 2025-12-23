package com.jjg.game.slots.game.goldsnakefortune.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoldSnakeFortuneGameManager extends AbstractGoldSnakeFortuneGameManager {
    public GoldSnakeFortuneGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }
}
