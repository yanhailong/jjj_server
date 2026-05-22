package com.jjg.game.sim.service;

import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.data.SimPlayerGameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private Logger log = LoggerFactory.getLogger(getClass());

    private final String TABLE_NAME = "simPlayerGameData";

    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 数据数据
     *
     * @param playerId
     * @param mongo
     * @return
     */
    public SimPlayerGameData getSimPlayerGameData(long playerId, boolean mongo) {
        SimPlayerGameData data = findFromRedis(playerId);
        if (data != null) {
            return data;
        }

        if (mongo) {
            Optional<SimPlayerGameData> optional = simPlayerGameDao.findById(playerId);
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }

    public SimPlayerGameData findFromRedis(long playerId) {
        return (SimPlayerGameData) redisTemplate.opsForHash().get(TABLE_NAME, playerId);
    }

    /**
     * 保存到redis
     *
     * @param simPlayerGameData
     */
    public void saveToRedis(SimPlayerGameData simPlayerGameData) {
        redisTemplate.opsForHash().put(TABLE_NAME, simPlayerGameData.getPlayerId(), simPlayerGameData);
    }


    public void moveToMongo(long playerId) {
        try {
            SimPlayerGameData data = findFromRedis(playerId);
            if (data == null) {
                return;
            }
            redisTemplate.opsForHash().delete(TABLE_NAME, playerId);
            simPlayerGameDao.save(data);

            log.info("将 SimPlayerGameData 保存到mongo成功, playerId = {}", playerId);
        } catch (Exception e) {
            log.error("将 SimPlayerGameData 保存到mongo出现异常", e);
        }
    }
}
