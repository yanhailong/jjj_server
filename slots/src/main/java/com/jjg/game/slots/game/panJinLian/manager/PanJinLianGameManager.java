package com.jjg.game.slots.game.panJinLian.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 潘金莲游戏逻辑管理器（个人模式）
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class PanJinLianGameManager extends AbstractPanJinLianGameManager {

    public PanJinLianGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }
}
