package com.jjg.game.slots.dao;

import com.jjg.game.slots.data.PlayerAllSlotsData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;


/**
 * @author 11
 * @date 2026/4/1
 */
@Repository
public class PlayerAllSlotsDataDao {
    private final String TABLE_NAME = "playerSlotsData";

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    public PlayerAllSlotsData getFromAllDB(long playerId) {
        PlayerAllSlotsData data = getFromRedis(playerId);
        if (data != null) {
            return data;
        }

        return mongoTemplate.findById(playerId, PlayerAllSlotsData.class);
    }

    public PlayerAllSlotsData getFromRedis(long playerId) {
        return (PlayerAllSlotsData) redisTemplate.opsForHash().get(TABLE_NAME, playerId);
    }

    public void saveToRedis(PlayerAllSlotsData data) {
        if(data == null) {
            return;
        }
        redisTemplate.opsForHash().put(TABLE_NAME, data.getPlayerId(), data);
    }

    public void moveToMongo(long playerId) {
        PlayerAllSlotsData data = getFromRedis(playerId);
        if (data != null) {
            mongoTemplate.save(data);
            redisTemplate.opsForHash().delete(TABLE_NAME, playerId);
        }
    }
}
