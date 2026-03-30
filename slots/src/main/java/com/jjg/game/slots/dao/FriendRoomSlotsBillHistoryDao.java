package com.jjg.game.slots.dao;

import com.jjg.game.core.manager.SnowflakeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class FriendRoomSlotsBillHistoryDao {

    private final String TABLE_NAME = "billHistory:";

    @Autowired
    private SnowflakeManager snowflakeManager;
    @Autowired
    private RedisTemplate redisTemplate;

    public String tableName(int gameType, int month) {
        return TABLE_NAME + gameType + ":" + month;
    }

    public long queryId(int gameType, int month, long creaor) {
        String tableName = tableName(gameType, month);
        Long existingId = (Long) redisTemplate.opsForHash().get(tableName, creaor);

        if (existingId != null) {
            return existingId;
        }

        long newId = snowflakeManager.nextId();
        Boolean success = redisTemplate.opsForHash().putIfAbsent(tableName, creaor, newId);
        return Boolean.TRUE.equals(success) ? newId : (Long) redisTemplate.opsForHash().get(tableName, creaor);
    }
}
