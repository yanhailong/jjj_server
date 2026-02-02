package com.jjg.game.slots.game.elephantgod.manager;

import com.jjg.game.slots.game.elephantgod.dao.ElephantGodGameDataDao;
import com.jjg.game.slots.game.elephantgod.dao.ElephantGodResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElephantGodGameManager extends AbstractElephantGodGameManager{
    public ElephantGodGameManager(ElephantGodResultLibDao libDao, ElephantGodGenerateManager generateManager, ElephantGodGameDataDao gameDataDao) {
        super(libDao, generateManager, gameDataDao);
        this.log = LoggerFactory.getLogger(getClass());

    }
}
