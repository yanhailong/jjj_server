package com.jjg.game.slots.game.pegasusunbridle.manager;

import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/18 16:11
 */
@Component
public class PegasusUnbridleGameManager extends AbstractPegasusUnbridleGameManager{
    public PegasusUnbridleGameManager(PegasusUnbridleGameGenerateManager gameGenerateManager, PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao) {
        super(gameGenerateManager, PegasusUnbridleResultLibDao);
        this.log =  LoggerFactory.getLogger(getClass());
    }
}
