package com.jjg.game.slots.game.tigerbringsriches.manager;

import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/18 16:11
 */
@Component
public class TigerBringsRichesGameManager extends AbstractTigerBringsRichesGameManager {
    public TigerBringsRichesGameManager(TigerBringsRichesGameGenerateManager gameGenerateManager, TigerBringsRichesResultLibDao TigerBringsRichesResultLibDao) {
        super(gameGenerateManager, TigerBringsRichesResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
