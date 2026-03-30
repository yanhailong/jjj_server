package com.jjg.game.slots.game.angrybirds.manager;


import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsGameDataDao;
import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class AngryBirdsGameManager extends AbstractAngryBirdsGameManager {
    public AngryBirdsGameManager(AngryBirdsGenerateManager gameGenerateManager,
                                 AngryBirdsGameDataDao gameDataDao, AngryBirdsResultLibDao angryBirdsResultLibDao) {
        super(gameGenerateManager, gameDataDao, angryBirdsResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
