package com.jjg.game.slots.game.angrybirds.manager;


import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class AngryBirdsGameManager extends AbstractAngryBirdsGameManager {
    public AngryBirdsGameManager(AngryBirdsGenerateManager gameGenerateManager,AngryBirdsResultLibDao angryBirdsResultLibDao) {
        super(gameGenerateManager, angryBirdsResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
