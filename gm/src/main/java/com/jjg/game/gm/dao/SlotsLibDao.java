package com.jjg.game.gm.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

/**
 * @author 11
 * @date 2025/11/4 10:24
 */
@Repository
public class SlotsLibDao {
    private Logger log = LoggerFactory.getLogger(getClass());

    //生成结果集的时候的锁名
    protected String generateLock = "generateLock:";

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 使用RedisTemplate扫描所有generateLock
     */
    public Set<Integer> scanAllGenerateLocks() {
        Set<Integer> lockedGameTypes = new HashSet<>();

        // 使用scan命令避免阻塞
        ScanOptions options = ScanOptions.scanOptions()
                .match(generateLock + ":*")
                .count(100) // 每次扫描100个
                .build();

        Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(options);

        while (cursor.hasNext()) {
            String key = new String(cursor.next());
            String[] parts = key.split(":");
            if (parts.length >= 2) {
                try {
                    int gameType = Integer.parseInt(parts[1]);
                    lockedGameTypes.add(gameType);
                } catch (NumberFormatException e) {
                    log.warn("解析gameType失败，键名: {}", key);
                }
            }
        }

        return lockedGameTypes;
    }

}
