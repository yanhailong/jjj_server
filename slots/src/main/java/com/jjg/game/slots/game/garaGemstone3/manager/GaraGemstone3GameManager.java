package com.jjg.game.slots.game.garaGemstone3.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GaraGemstone3GameManager extends AbstractGaraGemstone3GameManager {
    public GaraGemstone3GameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
