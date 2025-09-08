package com.jjg.game.activity.common.dao;

import com.jjg.game.activity.common.data.ActivityData;
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
    private static final Logger log = LoggerFactory.getLogger(ActivityDao.class);
    private static final String TABLE_NAME = "activity:server";

    private final RedisTemplate<String, ActivityData> redisTemplate;

    public ActivityDao(RedisTemplate<String, ActivityData> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private HashOperations<String, Long, ActivityData> opsForHash() {
        return redisTemplate.opsForHash();
    }

    public String getLockKey(long activityId) {
        return "activity:lock" + activityId;
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
    public void saveActivity(ActivityData config) {
        if (config == null || config.getId() == 0) {
            log.warn("保存活动配置失败: config 或 ID 为空");
            return;
        }
        try {
            opsForHash().put(TABLE_NAME, config.getId(), config);
        } catch (Exception e) {
            log.error("保存活动配置异常 id={}", config.getId(), e);
        }
    }

    /**
     * 批量保存活动配置
     */
    public void saveActivities(Map<Long, ActivityData> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        try {
            opsForHash().putAll(TABLE_NAME, configs);
        } catch (Exception e) {
            log.error("批量保存活动配置异常, size={}", configs.size(), e);
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
