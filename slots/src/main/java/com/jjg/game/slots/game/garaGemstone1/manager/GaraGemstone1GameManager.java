package com.jjg.game.slots.game.garaGemstone1.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GaraGemstone1GameManager extends AbstractGaraGemstone1GameManager {
    public GaraGemstone1GameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
