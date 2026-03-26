package com.jjg.game.slots.game.wolfmoon.manager;

import com.jjg.game.slots.game.wolfmoon.dao.WolfMoonResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Component
public class WolfMoonGameManager extends AbstractWolfMoonGameManager {

    public WolfMoonGameManager(WolfMoonGenerateManager generateManager, WolfMoonResultLibDao wolfMoonResultLibDao) {
        super(generateManager, wolfMoonResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
