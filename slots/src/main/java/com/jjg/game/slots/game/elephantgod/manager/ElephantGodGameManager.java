package com.jjg.game.slots.game.elephantgod.manager;

import com.jjg.game.slots.game.elephantgod.dao.ElephantGodResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElephantGodGameManager extends AbstractElephantGodGameManager{
    public ElephantGodGameManager(ElephantGodResultLibDao libDao, ElephantGodGenerateManager generateManager) {
        super(libDao, generateManager);
        this.log = LoggerFactory.getLogger(getClass());

    }
}
