package com.jjg.game.core.utils;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/9/23 09:38
 */
@Component
public class RedisUtils {

    /**
     * 通过匹配批量删除redis数据
     *
     * @param redisTemplate redis连接
     * @param pattern       匹配模式
     */
    public void deleteByPattern(RedisTemplate<?, ?> redisTemplate, String pattern) {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(
                    ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                List<byte[]> batch = new ArrayList<>();
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 500) {
                        connection.keyCommands().del(batch.toArray(new byte[0][]));
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    connection.keyCommands().del(batch.toArray(new byte[0][]));
                }
            }
            return null;
        });
    }
}
