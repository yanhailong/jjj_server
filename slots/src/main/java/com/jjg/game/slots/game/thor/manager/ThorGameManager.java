package com.jjg.game.slots.game.thor.manager;


import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Component
public class ThorGameManager extends AbstractThorGameManager {

    public ThorGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }
}
