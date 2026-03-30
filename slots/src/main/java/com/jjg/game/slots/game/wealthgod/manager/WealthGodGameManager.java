package com.jjg.game.slots.game.wealthgod.manager;


import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class WealthGodGameManager extends AbstractWealthGodGameManager {
    public WealthGodGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }
}
