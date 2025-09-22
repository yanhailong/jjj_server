package com.jjg.game.core.dao.luckytreasure;

import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.core.data.LuckyTreasureBuyRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 夺宝奇兵redis操作类
 */
@Component
public class LuckyTreasureRedisDao {

    private final RedisTemplate<String, Object> redisTemplate;

    public LuckyTreasureRedisDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     * 检查指定配置ID是否已有活跃的活动
     */
    public boolean hasActiveRound(int configId) {
        String configMappingKey = buildConfigMappingKey(configId);
        return redisTemplate.hasKey(configMappingKey);
    }

    /**
     * 保存活跃活动到Redis
     */
    public void saveActiveRound(LuckyTreasure luckyTreasure, int expireMinutes) {
        // 直接按期号存储活动数据，实现一次查询
        String issueKey = buildIssueMappingKey(luckyTreasure.getIssueNumber());
        redisTemplate.opsForValue().set(issueKey, luckyTreasure, expireMinutes, TimeUnit.MINUTES);

        // 同时维护configId到期号的映射，用于按配置ID查询
        String configMappingKey = buildConfigMappingKey(luckyTreasure.getConfig().getId());
        redisTemplate.opsForValue().set(configMappingKey, luckyTreasure.getIssueNumber(), expireMinutes, TimeUnit.MINUTES);
    }

    /**
     * 获取活跃活动（通过配置ID）
     */
    public LuckyTreasure getActiveRound(int configId) {
        String configMappingKey = buildConfigMappingKey(configId);
        Long issueNumber = (Long) redisTemplate.opsForValue().get(configMappingKey);

        if (issueNumber != null) {
            return getTreasureByIssueNumber(issueNumber);
        }

        return null;
    }

    /**
     * 根据Redis Key获取活跃活动
     */
    public LuckyTreasure getActiveRoundByKey(String redisKey) {
        Object obj = redisTemplate.opsForValue().get(redisKey);
        return obj instanceof LuckyTreasure ? (LuckyTreasure) obj : null;
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
        LuckyTreasure treasure = (LuckyTreasure) redisTemplate.opsForValue().get(issueKey);

        if (treasure != null) {
            // 删除期号Key
            redisTemplate.delete(issueKey);
            // 删除配置映射
            String configMappingKey = buildConfigMappingKey(treasure.getConfig().getId());
            redisTemplate.delete(configMappingKey);
        }
    }

    /**
     * 生成期号（原子递增）
     */
    public long generateIssueNumber(int configId, String today) {
        String key = buildDailyCounterKey(configId, today);
        Long counter = redisTemplate.opsForValue().increment(key);
        if (counter != null && counter == 1) {
            // 设置过期时间为第二天
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        return counter != null ? counter : 1L;
    }

    /**
     * 获取所有活跃活动的键
     */
    public List<String> getAllActiveRoundKeys() {
        String pattern = LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_ROUND_DATA_ISSUE + "*";
        return redisTemplate.keys(pattern).stream().toList();
    }

    /**
     * 获取所有活跃的夺宝奇兵活动
     */
    public List<LuckyTreasure> getActiveTreasures() {
        List<String> keys = getAllActiveRoundKeys();
        List<LuckyTreasure> treasures = new ArrayList<>();
        for (String key : keys) {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof LuckyTreasure) {
                treasures.add((LuckyTreasure) obj);
            }
        }
        return treasures;
    }

    /**
     * 根据期号获取夺宝奇兵活动（一次查询）
     */
    public LuckyTreasure getTreasureByIssueNumber(long issueNumber) {
        // 直接按期号获取活动数据，只需要一次Redis查询
        String issueKey = buildIssueMappingKey(issueNumber);
        Object obj = redisTemplate.opsForValue().get(issueKey);
        return obj instanceof LuckyTreasure ? (LuckyTreasure) obj : null;
    }

    /**
     * 购买夺宝奇兵（原子操作）
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
     * 记录数据变化的期号
     *
     * @param issueNumber 期号
     */
    @SuppressWarnings("unchecked")
    public void addUpdateInfo(long issueNumber) {
        Set<Long> issueNumberSet = (Set<Long>) redisTemplate.opsForValue().get(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_UPDATE_INFO);
        if (issueNumberSet == null) {
            issueNumberSet = new HashSet<>();
        }
        issueNumberSet.add(issueNumber);
        redisTemplate.opsForValue().set(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_UPDATE_INFO, issueNumberSet);

    }

    /**
     * 清除已经通知过更新的期号
     */
    public void removeUpdateInfo() {
        redisTemplate.delete(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_UPDATE_INFO);
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
