package com.jjg.game.activity.wealthroulette.dao;

import com.jjg.game.common.utils.TimeHelper;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

/**
 * 财富转盘用于记录玩家购买记录
 * @author lm
 * @date 2025/12/1 13:47
 */
@Repository
public class WealthRouletteDao {

    private final String tableName = "wealthroulette:%s";
    private final RedissonClient redissonClient;

    public WealthRouletteDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 增加每日购买次数
     * @param playerId 玩家id
     * @param goodId 商品id
     * @return 成功自增后的新值 (Long)，如果达到上限或键不存在，返回 null。
     */
    public Long incrementIfLessThan(long playerId, int goodId, long maxLimit) {
        // Lua 脚本：实现原子操作
        // KEYS[1]: Map Key (hash name)
        // ARGV[1]: Field Key (hash field)
        // ARGV[2]: incrementValue (自增的值)
        // ARGV[3]: maxLimit (上限值)
        String lua = """
                -- 1. 获取键的当前值 (使用 HGET)
                local current_str = redis.call('HGET', KEYS[1], ARGV[1])
                
                local current_val
                
                -- 2. 处理键不存在或值为空的情况
                if not current_str or current_str == '' then
                    -- 如果键不存在，我们默认值为 0
                    current_val = 0
                else
                    -- 转换为数字
                    current_val = tonumber(current_str)
                end
                
                -- 3. 条件判断：如果当前值加自增值 >= 上限，则返回 nil
                local amount = tonumber(ARGV[2])
                local max_limit = tonumber(ARGV[3])
                if current_val+amount > max_limit then
                    return nil
                end
                
                -- 4. 执行原子自增操作 (使用 HINCRBY)
                local new_val = redis.call('HINCRBY', KEYS[1], ARGV[1], amount)
                -- 6. 返回自增后的新值
                return new_val
                """;

        // 转换为 Long，用于 Redis 的 HINCRBY
        // 执行 Lua 脚本
        return redissonClient.getScript(StringCodec.INSTANCE)
                .eval(
                        RScript.Mode.READ_WRITE,
                        lua,
                        RScript.ReturnType.INTEGER,
                        Collections.singletonList(tableName.formatted(playerId)),
                        String.valueOf(goodId),
                        String.valueOf(1),
                        String.valueOf(maxLimit)
                );
    }

    /**
     * 获取今天玩家的购买次数
     * @param playerId 玩家id
     */
    public RMap<Integer, Integer> getPlayerBuyTimes(long playerId) {
        return redissonClient.getMap(tableName.formatted(playerId));
    }


    /**
     * 是否能触发首次登陆
     * @param playerId 玩家id
     * @return true能触发
     */
    public boolean canTargetFirstLogin(long playerId) {
        RBucket<String> bucket = redissonClient.getBucket(tableName.formatted(playerId) + "lock");
        return bucket.setIfAbsent(TimeHelper.FORMATTER.format(LocalDate.now()), Duration.of(10, ChronoUnit.SECONDS));
    }

}
