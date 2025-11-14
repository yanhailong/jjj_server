package com.jjg.game.core.dao.luckytreasure;

import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.core.data.LuckyTreasureBuyRecord;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 夺宝奇兵redis操作类
 */
@Component
public class LuckyTreasureRedisDao {

    private final RedissonClient redissonClient;

    public LuckyTreasureRedisDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }


    /**
     * 检查指定配置ID是否已有活跃的活动
     */
    public boolean hasActiveRound(int configId) {
        String configMappingKey = buildConfigMappingKey(configId);
        RBucket<Long> bucket = redissonClient.getBucket(configMappingKey);
        return bucket.isExists();
    }

    /**
     * 保存活跃活动到Redis
     */
    public void saveActiveRound(LuckyTreasure luckyTreasure, int expireMinutes) {
        // 直接按期号存储活动数据，实现一次查询
        String issueKey = buildIssueMappingKey(luckyTreasure.getIssueNumber());
        RBucket<LuckyTreasure> issueBucket = redissonClient.getBucket(issueKey);
        issueBucket.set(luckyTreasure);
        issueBucket.expire(Duration.ofMinutes(expireMinutes));

        // 同时维护configId到期号的映射，用于按配置ID查询
        String configMappingKey = buildConfigMappingKey(luckyTreasure.getConfig().getId());
        RBucket<Long> configBucket = redissonClient.getBucket(configMappingKey);
        configBucket.set(luckyTreasure.getIssueNumber());
        configBucket.expire(Duration.ofMinutes(expireMinutes));
    }

    /**
     * 获取活跃活动（通过配置ID）
     */
    public LuckyTreasure getActiveRound(int configId) {
        String configMappingKey = buildConfigMappingKey(configId);
        RBucket<Long> configBucket = redissonClient.getBucket(configMappingKey);
        Long issueNumber = configBucket.get();

        if (issueNumber != null) {
            return getTreasureByIssueNumber(issueNumber);
        }

        return null;
    }

    /**
     * 根据Redis Key获取活跃活动
     */
    public LuckyTreasure getActiveRoundByKey(String redisKey) {
        RBucket<LuckyTreasure> bucket = redissonClient.getBucket(redisKey);
        return bucket.get();
    }

    /**
     * 更新活跃活动
     */
    public void updateActiveRound(LuckyTreasure luckyTreasure, int expireMinutes) {
        saveActiveRound(luckyTreasure, expireMinutes);
    }

    /**
     * 删除活跃活动状态（通过期号）
     */
    public void removeActiveRoundByIssueNumber(long issueNumber) {
        String issueKey = buildIssueMappingKey(issueNumber);
        RBucket<LuckyTreasure> issueBucket = redissonClient.getBucket(issueKey);
        LuckyTreasure treasure = issueBucket.get();

        if (treasure != null) {
            // 删除期号Key
            issueBucket.delete();
            // 删除配置映射
            String configMappingKey = buildConfigMappingKey(treasure.getConfig().getId());
            RBucket<Long> configBucket = redissonClient.getBucket(configMappingKey);
            configBucket.delete();
        }
    }

    /**
     * 生成期号（原子递增）
     */
    public long generateIssueNumber(int configId, String today) {
        String key = buildDailyCounterKey(configId, today);
        RAtomicLong counter = redissonClient.getAtomicLong(key);

        // 使用原子操作递增
        long result = counter.incrementAndGet();
        if (result == 1) {
            // 设置过期时间为第二天
            counter.expire(Duration.ofHours(24));
        }

        return result;
    }

    /**
     * 获取所有活跃活动的键
     */
    public List<String> getAllActiveRoundKeys() {
        String pattern = LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_ROUND_DATA_ISSUE + "*";
        KeysScanOptions defaults = KeysScanOptions.defaults();
        defaults.chunkSize(1000);
        defaults.limit(10);
        defaults.pattern(pattern);
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keyIterable = keys.getKeys(defaults);
        List<String> keyList = new ArrayList<>();
        for (String key : keyIterable) {
            keyList.add(key);
        }
        return keyList;
    }

    /**
     * 获取所有活跃的夺宝奇兵活动
     */
    public List<LuckyTreasure> getActiveTreasures() {
        List<String> keys = getAllActiveRoundKeys();
        return keys.stream()
                .map(key -> {
                    RBucket<LuckyTreasure> bucket = redissonClient.getBucket(key);
                    return bucket.get();
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据期号获取夺宝奇兵活动（一次查询）
     */
    public LuckyTreasure getTreasureByIssueNumber(long issueNumber) {
        // 直接按期号获取活动数据，只需要一次Redis查询
        String issueKey = buildIssueMappingKey(issueNumber);
        RBucket<LuckyTreasure> bucket = redissonClient.getBucket(issueKey);
        return bucket.get();
    }

    /**
     * 购买夺宝奇兵
     */
    public LuckyTreasure buyTreasure(long issueNumber, long playerId, int count) {
        LuckyTreasure treasure = getTreasureByIssueNumber(issueNumber);
        if (treasure != null) {
            // 检查剩余数量
            int remainingCount = treasure.getConfig().getTotal() - treasure.getSoldCount();
            if (remainingCount >= count) {
                // 更新购买数据
                treasure.getBuyMap().merge(playerId, count, Integer::sum);
                treasure.setSoldCount(treasure.getSoldCount() + count);
                //增加购买记录
                LuckyTreasureBuyRecord buyRecord = new LuckyTreasureBuyRecord();
                buyRecord.setBuyCount(count);
                buyRecord.setPlayerId(playerId);
                buyRecord.setBuyTime(System.currentTimeMillis());
                treasure.getBuyRecordList().add(buyRecord);
                int expireMinutes = treasure.getConfig().getTime() + treasure.getConfig().getCollectTime();
                // 保存回Redis
                updateActiveRound(treasure, expireMinutes);
                return treasure;
            }
        }
        return null;
    }

    /**
     * 构建每日计数器Redis Key
     */
    private String buildDailyCounterKey(int configId, String date) {
        return LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_DAILY_COUNTER + configId + ":" + date;
    }

    /**
     * 构建期号映射Redis Key
     */
    private String buildIssueMappingKey(long issueNumber) {
        return LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_ROUND_DATA_ISSUE + issueNumber;
    }

    /**
     * 构建配置映射Redis Key
     */
    private String buildConfigMappingKey(int configId) {
        return LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_ACTIVE + configId;
    }
}
