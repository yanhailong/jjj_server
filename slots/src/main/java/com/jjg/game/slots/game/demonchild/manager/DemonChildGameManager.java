package com.jjg.game.slots.game.demonchild.manager;


import com.jjg.game.slots.game.demonchild.dao.DemonChildGameDataDao;
import com.jjg.game.slots.game.demonchild.dao.DemonChildResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class DemonChildGameManager extends AbstractDemonChildGameManager {
    public DemonChildGameManager(DemonChildGameGenerateManager gameGenerateManager,
                                 DemonChildGameDataDao gameDataDao, DemonChildResultLibDao demonChildResultLibDao) {
        super(gameGenerateManager, gameDataDao, demonChildResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
