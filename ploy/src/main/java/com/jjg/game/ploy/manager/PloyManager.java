package com.jjg.game.ploy.manager;

import com.jjg.game.ploy.constant.PloyGameType;
import com.jjg.game.ploy.dao.PloyPoolDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 小游戏模块总线
 *
 * @author 11
 * @date 2026/3/19
 */
@Component
public class PloyManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PloyPoolDao poolDao;

    public void init() {
        //游戏类型初始化
        PloyGameType.init();
        //奖池初始化
        poolDao.initPool();
    }

    public void shutdown() {
        for (PloyGameType ployGame : PloyGameType.values()) {
            ployGame.getController().shutdown();
        }
    }
}
