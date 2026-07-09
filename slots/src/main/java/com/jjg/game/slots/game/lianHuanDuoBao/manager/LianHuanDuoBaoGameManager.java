package com.jjg.game.slots.game.lianHuanDuoBao.manager;

import com.jjg.game.slots.game.lianHuanDuoBao.dao.LianHuanDuoBaoResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2026/6/2
 */
//TODO 连环夺宝配表完成后取消注释即可启用
//@Component
public class LianHuanDuoBaoGameManager extends AbstractLianHuanDuoBaoGameManager {
    public LianHuanDuoBaoGameManager(LianHuanDuoBaoGameGenerateManager gameGenerateManager,
                                     LianHuanDuoBaoResultLibDao resultLibDao) {
        super(gameGenerateManager, resultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
