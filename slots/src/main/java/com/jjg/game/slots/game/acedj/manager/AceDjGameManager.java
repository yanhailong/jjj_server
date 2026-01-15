package com.jjg.game.slots.game.acedj.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * 王牌Dj游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class AceDjGameManager extends AbstractAceDjGameManager {
    public AceDjGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
