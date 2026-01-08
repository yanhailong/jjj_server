package com.jjg.game.slots.game.elephantgod.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElephantGodGameManager extends AbstractElephantGodGameManager{
    public ElephantGodGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
