package com.jjg.game.core.utils;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * 保留两位小数计算
     * @param value 要转换的值
     * @return 转换后的值
     */
    // ---------- 工具 ----------
    public static long toLong(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
    }

    /**
     * 把值转为带小数的值
     * @param value 转换的值
     * @return 带两位小数的值
     */
    public static BigDecimal fromLong(long value) {
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
