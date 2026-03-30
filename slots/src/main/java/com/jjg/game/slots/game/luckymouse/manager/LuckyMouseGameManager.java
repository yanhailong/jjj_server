package com.jjg.game.slots.game.luckymouse.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LuckyMouseGameManager extends AbstractLuckyMouseGameManager{
    public LuckyMouseGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
