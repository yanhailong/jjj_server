package com.jjg.game.slots.game.steamAge.manager;

import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.steamAge.dao.SteamAgeGameDataDao;
import com.jjg.game.slots.game.steamAge.dao.SteamAgeResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 蒸汽时代游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class SteamAgeGameManager extends AbstractSteamAgeGameManager {

    @Autowired
    private SteamAgeResultLibDao libDao;
    @Autowired
    private SteamAgeGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SteamAgeGameDataDao gameDataDao;

    public SteamAgeGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }




}
