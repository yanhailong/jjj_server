package com.jjg.game.slots.game.pegasusunbridle.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleGameDataDao;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleResultLibDao;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleGameRunInfo;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;

/**
 * @author lm
 * @date 2025/12/18 16:11
 */
@Component
public class PegasusUnbridleGameManager extends AbstractPegasusUnbridleGameManager{
    public PegasusUnbridleGameManager(PegasusUnbridleGameGenerateManager gameGenerateManager, PegasusUnbridleGameDataDao gameDataDao, PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao) {
        super(gameGenerateManager, gameDataDao, PegasusUnbridleResultLibDao);
    }
}
