package com.jjg.game.activity.common.dao;

import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.redis.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动配置DAO
 * 用于操作 Redis 中的活动配置表：activity:server
 *
 * @author lm
 * @date 2025/9/4
 */
@Repository
public class ActivityDao {
    private final Logger log = LoggerFactory.getLogger(ActivityDao.class);
    private final String TABLE_NAME = "activity:server";
    private final String ACTIVITY_ALL_LOCK = "activity:alllock";
    private final String ACTIVITY_LOCK = "activity:lock:%d";
    private final RedisTemplate<String, ActivityData> redisTemplate;
    private final RedisLock redisLock;

    public ActivityDao(RedisTemplate<String, ActivityData> redisTemplate, RedisLock redisLock) {
        this.redisTemplate = redisTemplate;
        this.redisLock = redisLock;
    }

    private HashOperations<String, Long, ActivityData> opsForHash() {
        return redisTemplate.opsForHash();
    }

    public String getLockKey(long activityId) {
        return ACTIVITY_LOCK.formatted(activityId);
    }

    public String getAllLockKey() {
        return ACTIVITY_ALL_LOCK;
    }


    /**
     * 获取全部活动数据
     */
    public List<ActivityData> getAllActivityInfos() {
        try {
            return opsForHash().values(TABLE_NAME);
        } catch (Exception e) {
            log.error("获取全部活动配置异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据活动ID获取单个活动配置
     */
    public ActivityData getActivityById(long activityId) {
        try {
            return opsForHash().get(TABLE_NAME, activityId);
        } catch (Exception e) {
            log.error("获取活动配置异常 activityId={}", activityId, e);
            return null;
        }
    }

    /**
     * 批量获取活动配置
     */
    public Map<Long, ActivityData> getActivitiesByIds(List<Long> activityIds) {
        try {
            if (activityIds == null || activityIds.isEmpty()) {
                return Map.of();
            }
            List<ActivityData> list = opsForHash().multiGet(TABLE_NAME, new ArrayList<>(activityIds));
            Map<Long, ActivityData> result = new HashMap<>();
            for (int i = 0; i < activityIds.size(); i++) {
                if (list.get(i) != null) {
                    result.put(activityIds.get(i), list.get(i));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("批量获取活动配置异常 ids={}", activityIds, e);
            return Map.of();
        }
    }

    /**
     * 保存或更新单个活动配置
     */
    public void saveActivity(ActivityData data) {
        if (data == null || data.getId() == 0) {
            log.warn("保存活动配置失败: config 或 ID 为空");
            return;
        }
        String lockKey = getLockKey(data.getId());
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            opsForHash().put(TABLE_NAME, data.getId(), data);
        } catch (Exception e) {
            log.error("保存活动配置异常 id={}", data.getId(), e);
        } finally {
            redisLock.unlock(lockKey);
        }
    }

    /**
     * 批量保存活动配置
     */
    public void saveActivities(Map<Long, ActivityData> activityDataMap) {
        if (activityDataMap == null || activityDataMap.isEmpty()) {
            return;
        }
        String allLockKey = getAllLockKey();
        redisLock.lock(allLockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            opsForHash().putAll(TABLE_NAME, activityDataMap);
        } catch (Exception e) {
            log.error("批量保存活动配置异常, size={}", activityDataMap.size(), e);
        } finally {
            redisLock.unlock(allLockKey);
        }
    }

    /**
     * 删除单个活动配置
     */
    public void deleteActivity(long activityId) {
        try {
            opsForHash().delete(TABLE_NAME, activityId);
        } catch (Exception e) {
            log.error("删除活动配置异常 id={}", activityId, e);
        }
    }

    /**
     * 清空所有活动配置
     */
    public void clearAllActivities() {
        try {
            redisTemplate.delete(TABLE_NAME);
        } catch (Exception e) {
            log.error("清空活动配置异常", e);
        }
    }
}
