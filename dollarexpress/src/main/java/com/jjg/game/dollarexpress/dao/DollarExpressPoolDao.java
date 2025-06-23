package com.jjg.game.dollarexpress.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.sample.DollarExpressWareHouseConfig;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/13 16:00
 */
@Component
public class DollarExpressPoolDao extends AbstractPoolDao {

    @Override
    public void initPool() {
        for(int gameType : DollarExpressConst.GameType.SUPPORT_GAME_TYPES){
            for(DollarExpressWareHouseConfig config : DollarExpressWareHouseConfig.factory.getAllSamples()){
                redisTemplate.opsForHash().putIfAbsent(pool_prefix + gameType,config.getSid(),config.getBasicWarehouse());
            }
        }
    }
}
