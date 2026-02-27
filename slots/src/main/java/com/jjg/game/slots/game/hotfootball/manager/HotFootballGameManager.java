package com.jjg.game.slots.game.hotfootball.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 火热足球游戏逻辑处理器
 *
 * @author 11
 * @date 2025/8/1 17:25
 */
@Component
public class HotFootballGameManager extends AbstractHotFootballGameManager {
    public HotFootballGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
