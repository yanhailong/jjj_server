package com.jjg.game.sim.service;

import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.data.SimPlayerGameData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author 11
 * @date 2026/5/18
 */
@Service
public class SimPlayerGameDataService {

    private final String TABLE_NAME = "simPlayerGameData";

    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 数据数据
     * @param playerId
     * @param mongo
     * @return
     */
    public SimPlayerGameData getSimPlayerGameData(long playerId, boolean mongo) {
        Object o = redisTemplate.opsForHash().get(TABLE_NAME, playerId);
        if (o != null) {
            return (SimPlayerGameData) o;
        }

        if (mongo) {
            Optional<SimPlayerGameData> optional = simPlayerGameDao.findById(playerId);
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }

    public void saveToRedis(SimPlayerGameData simPlayerGameData) {
        redisTemplate.opsForHash().put(TABLE_NAME, simPlayerGameData.getId(), simPlayerGameData);
    }


}
