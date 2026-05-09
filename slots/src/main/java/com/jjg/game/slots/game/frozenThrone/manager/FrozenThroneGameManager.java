package com.jjg.game.slots.game.frozenThrone.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 寒冰王座游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class FrozenThroneGameManager extends AbstractFrozenThroneGameManager {

    public FrozenThroneGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }


}
