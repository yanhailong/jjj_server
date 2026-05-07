package com.jjg.game.slots.game.garaGemstone2.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GaraGemstone2GameManager extends AbstractGaraGemstone2GameManager {
    public GaraGemstone2GameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
