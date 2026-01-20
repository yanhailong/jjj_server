package com.jjg.game.slots.game.tenfoldgoldenbull.manager;

import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullGameDataDao;
import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/18 16:11
 */
@Component
public class TenFoldGoldenBullGameManager extends AbstractTenFoldGoldenBullGameManager {
    public TenFoldGoldenBullGameManager(TenFoldGoldenBullGameGenerateManager gameGenerateManager, TenFoldGoldenBullGameDataDao gameDataDao, TenFoldGoldenBullResultLibDao TenFoldGoldenBullResultLibDao) {
        super(gameGenerateManager, gameDataDao, TenFoldGoldenBullResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
