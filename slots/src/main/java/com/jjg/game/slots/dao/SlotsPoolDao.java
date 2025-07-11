package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.BaseRoomCfg;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2025/7/10 13:36
 */
@Component
public class SlotsPoolDao extends AbstractPoolDao {
    /**
     * 初始化水池
     */
    @Override
    public void initPool() {
        for(Map.Entry<Integer, BaseRoomCfg> en : GameDataManager.getBaseRoomCfgMap().entrySet()){
            BaseRoomCfg cfg = en.getValue();
            String tableName = tableName(cfg.getGameType());
            this.redisTemplate.opsForHash().putIfAbsent(tableName, cfg.getRoomName(), cfg.getInitBasePool());
        }
    }
}
