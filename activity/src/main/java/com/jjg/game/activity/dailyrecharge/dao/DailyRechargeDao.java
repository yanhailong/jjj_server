package com.jjg.game.activity.dailyrecharge.dao;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * @author lm
 * @date 2025/12/3 16:47
 */
@Repository
public class DailyRechargeDao {
    private final RedissonClient redissonClient;
    private final String BASE_KEY = "activity:dailyrecharge:%d:%d";

    public DailyRechargeDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 添加每天购买次数
     * @param playerId 玩家id
     * @param activityId 活动id
     * @param giftId 礼包id
     * @return 购买后的次数
     */
    public long addBuyTimes(long playerId, long activityId, int giftId) {
        String key = BASE_KEY.formatted(playerId, activityId);
        RMap<Integer, Integer> map = redissonClient.getMap(key);
        Integer addAndGet = map.addAndGet(giftId, 1);
        long addAfter = addAndGet == null ? 0 : addAndGet;
        if (addAfter == 1) {
            map.expire(Duration.of(1, ChronoUnit.DAYS));
        }
        return addAndGet == null ? 0 : addAndGet;
    }

    public void delete(long playerId, long activityId) {
        String key = BASE_KEY.formatted(playerId, activityId);
        redissonClient.getKeys().delete(key);
    }

    /**
     * 获取礼包购买次数
     * @param playerId 玩家id
     * @param activityId 活动id
     * @param giftId 礼包id
     * @return 购买次数
     */
    public int getBuyTimes(long playerId, long activityId, int giftId) {
        String key = BASE_KEY.formatted(playerId, activityId);
        RMap<Integer, Integer> map = redissonClient.getMap(key);
        Integer times = map.get(giftId);
        return times == null ? 0 : times;
    }

    /**
     * 获取所有礼包购买次数
     * @param playerId 玩家id
     * @param activityId 活动id
     * @return 所有礼包购买次数
     */
    public Map<Integer, Integer> getAllBuyTimes(long playerId, long activityId) {
        String key = BASE_KEY.formatted(playerId, activityId);
        RMap<Integer, Integer> objectRMap = redissonClient.getMap(key);
        return objectRMap.readAllMap();
    }
}
