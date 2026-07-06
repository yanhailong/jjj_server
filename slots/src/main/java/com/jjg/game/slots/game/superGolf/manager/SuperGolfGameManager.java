package com.jjg.game.slots.game.superGolf.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SuperGolfGameManager extends AbstractSuperGolfGameManager {
    public SuperGolfGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
