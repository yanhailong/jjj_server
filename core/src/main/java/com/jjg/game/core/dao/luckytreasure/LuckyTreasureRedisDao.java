package com.jjg.game.core.dao.luckytreasure;

import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.core.data.LuckyTreasureBuyRecord;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
        return getActiveConfigMap().containsKey(configId);
    }

    public RMapCache<Long, LuckyTreasure> getActiveTreasures() {
        return redissonClient.getMapCache(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_ROUND_DATA_ISSUE);
    }

    public RMapCache<Integer, Long> getActiveConfigMap() {
        return redissonClient.getMapCache(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_ACTIVE);
    }

    /**
     * 保存活跃活动到Redis
     */
    public void saveActiveRound(LuckyTreasure luckyTreasure, int expireMinutes) {
        RMapCache<Long, LuckyTreasure> cacheMap = getActiveTreasures();
        //保存并且设置过期
        cacheMap.put(luckyTreasure.getIssueNumber(), luckyTreasure, expireMinutes, TimeUnit.MINUTES);

        // 同时维护configId到期号的映射，用于按配置ID查询
        RMapCache<Integer, Long> configMap = getActiveConfigMap();
        configMap.put(luckyTreasure.getConfig().getId(), luckyTreasure.getIssueNumber(), expireMinutes, TimeUnit.MINUTES);

        // 直接按期号存储活动数据，实现一次查询
//        String issueKey = buildIssueMappingKey(luckyTreasure.getIssueNumber());
//        RBucket<LuckyTreasure> issueBucket = redissonClient.getBucket(issueKey);
//        issueBucket.set(luckyTreasure);
//        issueBucket.expire(Duration.ofMinutes(expireMinutes));
//
//        // 同时维护configId到期号的映射，用于按配置ID查询
//        String configMappingKey = buildConfigMappingKey(luckyTreasure.getConfig().getId());
//        RBucket<Long> configBucket = redissonClient.getBucket(configMappingKey);
//        configBucket.set(luckyTreasure.getIssueNumber());
//        configBucket.expire(Duration.ofMinutes(expireMinutes));
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
        RMapCache<Long, LuckyTreasure> activeTreasuresMap = getActiveTreasures();
        LuckyTreasure treasure = activeTreasuresMap.get(issueNumber);

        if (treasure != null) {
            // 删除期号Key
            activeTreasuresMap.remove(issueNumber);
            // 删除配置映射
            getActiveConfigMap().remove(treasure.getConfig().getId());
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
     * 根据期号获取夺宝奇兵活动（一次查询）
     */
    public LuckyTreasure getTreasureByIssueNumber(long issueNumber) {
        // 直接按期号获取活动数据，只需要一次Redis查询
//        String issueKey = buildIssueMappingKey(issueNumber);
//        RBucket<LuckyTreasure> bucket = redissonClient.getBucket(issueKey);
//        return bucket.get();
        return getActiveTreasures().get(issueNumber);
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

                int expireMinutes = 0;
                if (treasure.getEndTime() > 0) {
                    expireMinutes = treasure.getConfig().getTime()
                            + calculateRewardDelayExpireMinutes()
                            + treasure.getConfig().getCollectTime();
                }
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

    private int calculateRewardDelayExpireMinutes() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_REWARED_CONFIG_ID);
        if (globalConfigCfg == null || globalConfigCfg.getIntValue() <= 0) {
            return 0;
        }
        long rewardDelayMillis = TimeUnit.SECONDS.toMillis(globalConfigCfg.getIntValue());
        //毫秒转分钟并向上取整
        return (int) ((rewardDelayMillis + TimeUnit.MINUTES.toMillis(1) - 1) / TimeUnit.MINUTES.toMillis(1));
    }

}
