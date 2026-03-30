package com.jjg.game.slots.game.christmasBashNight.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * 圣诞狂欢夜游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class ChristmasBashNightGameManager extends AbstractChristmasBashNightGameManager {
    public ChristmasBashNightGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
