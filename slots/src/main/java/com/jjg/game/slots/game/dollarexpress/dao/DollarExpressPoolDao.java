package com.jjg.game.slots.game.dollarexpress.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.slots.constant.SlotsConst;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2025/6/13 16:00
 */
@Component
public class DollarExpressPoolDao extends AbstractPoolDao {

    @Override
    public void initPool() {
        for(int gameType : SlotsConst.GameType.SUPPORT_GAME_TYPES){

//            for(Map.Entry<Integer, DollarExpressWareHouseCfg> en : GameDataManager.getDollarExpressWareHouseCfgMap().entrySet()){
//                DollarExpressWareHouseCfg cfg = en.getValue();
//                redisTemplate.opsForHash().putIfAbsent(pool_prefix + gameType,cfg.getSid(),cfg.getBasicWarehouse());
//            }
        }
    }
}
