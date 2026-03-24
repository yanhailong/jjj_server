package com.jjg.game.slots.game.findgoldcity.manager;

import com.jjg.game.slots.game.findgoldcity.dao.FindGoldCityResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/18 16:11
 */
@Component
public class FindGoldCityGameManager extends AbstractFindGoldCityGameManager {
    public FindGoldCityGameManager(FindGoldCityGameGenerateManager gameGenerateManager, FindGoldCityResultLibDao FindGoldCityResultLibDao) {
        super(gameGenerateManager, FindGoldCityResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
