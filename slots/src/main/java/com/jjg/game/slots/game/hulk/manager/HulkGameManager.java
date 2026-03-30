package com.jjg.game.slots.game.hulk.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/1/15
 */
@Component
public class HulkGameManager extends AbstractHulkGameManager{
    public HulkGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }
}
