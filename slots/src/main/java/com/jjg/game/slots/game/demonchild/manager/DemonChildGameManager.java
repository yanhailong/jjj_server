package com.jjg.game.slots.game.demonchild.manager;


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
                                  DemonChildResultLibDao demonChildResultLibDao) {
        super(gameGenerateManager, demonChildResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
