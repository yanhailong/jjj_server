package com.jjg.game.ploy.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2026/3/19
 */
@Component
public class PloyPoolDao extends AbstractPoolDao {
    /**
     * 初始化奖池
     */
    public void initPool(){
        for (Map.Entry<Integer, PloygameRoomCfg> en : GameDataManager.getPloygameRoomCfgMap().entrySet()) {
            PloygameRoomCfg cfg = en.getValue();
            this.redisTemplate.opsForHash().putIfAbsent(tableName(cfg.getGameType()), cfg.getId(), cfg.getInitBasePool());
        }
    }
}
