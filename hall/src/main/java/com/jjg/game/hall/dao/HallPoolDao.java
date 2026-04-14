package com.jjg.game.hall.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.hall.logger.HallLogger;
import com.jjg.game.sampledata.GameDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/6/18 16:18
 */
@Component
public class HallPoolDao extends AbstractPoolDao {
    @Autowired
    private HallLogger hallLogger;


    public void snapshot() {
        try {
            Set<Integer> gameTypeSet = new HashSet<>();
            GameDataManager.getWarehouseCfgMap().forEach((k, v) -> {
                gameTypeSet.add(v.getGameID());
            });

            if (gameTypeSet.isEmpty()) {
                log.warn("获取 gameType 失败，奖池快照失败");
                return;
            }

            Map<Integer, Map<Integer, Long>> pools = getBigPools(new ArrayList<>(gameTypeSet));
            if (pools == null || pools.isEmpty()) {
                log.warn("从redis获取奖池数据失败，奖池快照失败");
                return;
            }

            Map<Integer, Long> poolMap = new HashMap<>();
            pools.forEach((k, v) -> {
                poolMap.putAll(v);
            });

            hallLogger.pool(poolMap);
            log.info("奖池快照完成，记录 {} 条数据", poolMap.size());
        } catch (Exception e) {
            log.error("奖池快照异常", e);
        }
    }
}
