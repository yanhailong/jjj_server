package com.jjg.game.slots.game.wolfmoon.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WolfMoonGameManager extends AbstractWolfMoonGameManager {
    public WolfMoonGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
