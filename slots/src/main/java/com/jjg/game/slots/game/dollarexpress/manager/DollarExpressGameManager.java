package com.jjg.game.slots.game.dollarexpress.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 美元快递游戏逻辑处理器
 *
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressGameManager extends AbstractDollarExpressGameManager {
    public DollarExpressGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
