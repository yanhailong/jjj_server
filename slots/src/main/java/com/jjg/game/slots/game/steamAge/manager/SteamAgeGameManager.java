package com.jjg.game.slots.game.steamAge.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 蒸汽时代游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class SteamAgeGameManager extends AbstractSteamAgeGameManager {

    public SteamAgeGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

}
