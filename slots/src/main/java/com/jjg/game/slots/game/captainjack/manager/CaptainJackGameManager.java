package com.jjg.game.slots.game.captainjack.manager;


import com.jjg.game.slots.game.captainjack.dao.CaptainJackGameDataDao;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class CaptainJackGameManager extends AbstractCaptainJackGameManager {
    public CaptainJackGameManager(CaptainJackGameGenerateManager gameGenerateManager,
                                  CaptainJackGameDataDao gameDataDao, CaptainJackResultLibDao captainJackResultLibDao) {
        super(gameGenerateManager, gameDataDao, captainJackResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
