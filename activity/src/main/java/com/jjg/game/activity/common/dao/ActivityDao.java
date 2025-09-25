package com.jjg.game.activity.common.dao;

import com.jjg.game.activity.common.data.ActivityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 活动配置 DAO
 *
 * <p>功能：
 * - 查询：单个 / 批量 / 全部
 * - 保存：单个 / 批量
 * - 删除：单个 / 全部
 * - 使用 RedisLock 保证并发安全
 * <p>
 * 存储结构：
 * Redis Hash
 * key   = "activity:server"
 * field = 活动ID (Long)
 * value = ActivityData
 *
 * @author lm
 * @date 2025/9/4
 */
@Repository
public class ActivityDao {
    private static final Logger log = LoggerFactory.getLogger(ActivityDao.class);

    /**
     * Redis Hash 存储活动配置的表名
     */
    private static final String TABLE_NAME = "activity:server";

    /**
     * 单个活动配置锁 key 模板
     */
    private static final String ACTIVITY_LOCK = "activity:lock:%s";

    private final RedisTemplate<String, ActivityData> redisTemplate;

    public ActivityDao(RedisTemplate<String, ActivityData> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private HashOperations<String, Long, ActivityData> opsForHash() {
        return redisTemplate.opsForHash();
    }


    /**
     * 获取全部活动配置
     *
     * @return 活动配置列表，失败时返回空列表
     */
    public List<ActivityData> getAllActivityInfos() {
        try {
            return opsForHash().values(TABLE_NAME);
        } catch (Exception e) {
            log.error("获取全部活动配置失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据活动ID获取单个活动配置
     *
     * @param activityId 活动ID
     * @return 活动配置，失败时返回 null
     */
    public ActivityData getActivityById(long activityId) {
        try {
            return opsForHash().get(TABLE_NAME, activityId);
        } catch (Exception e) {
            log.error("获取活动配置失败 activityId={}", activityId, e);
            return null;
        }
    }

    /**
     * 批量获取活动配置
     *
     * @param activityIds 活动ID列表
     * @return 活动ID -> 活动配置 Map，失败或为空返回空Map
     */
    public Map<Long, ActivityData> getActivitiesByIds(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<ActivityData> list = opsForHash().multiGet(TABLE_NAME, new ArrayList<>(activityIds));
            Map<Long, ActivityData> result = new HashMap<>(activityIds.size());
            for (int i = 0; i < activityIds.size(); i++) {
                ActivityData data = i < list.size() ? list.get(i) : null;
                if (data != null) {
                    result.put(activityIds.get(i), data);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("批量获取活动配置失败 ids={}", activityIds, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 保存或更新单个活动配置
     *
     * @param data 活动配置（不能为空，ID 必须大于0）
     */
    public void saveActivity(ActivityData data) {
        if (data == null || data.getId() == 0) {
            log.warn("保存活动配置失败: 数据或ID为空");
            return;
        }
        try {
            opsForHash().put(TABLE_NAME, data.getId(), data);
        } catch (Exception e) {
            log.error("保存活动配置失败 id={}", data.getId(), e);
        }
    }

    /**
     * 批量保存活动配置
     *
     * @param activityDataMap 活动配置 Map，key=活动ID，value=配置
     */
    public void saveActivities(Map<Long, ActivityData> activityDataMap) {
        if (activityDataMap == null || activityDataMap.isEmpty()) {
            return;
        }
        try {
            opsForHash().putAll(TABLE_NAME, activityDataMap);
        } catch (Exception e) {
            log.error("批量保存活动配置失败, size={}", activityDataMap.size(), e);
        }
    }

    /**
     * 删除单个活动配置
     *
     * @param activityId 活动ID
     */
    public boolean deleteActivity(long activityId) {
        try {
            return opsForHash().delete(TABLE_NAME, activityId) > 0;
        } catch (Exception e) {
            log.error("删除活动配置失败 id={}", activityId, e);
        }
        return false;
    }

    /**
     * 清空所有活动配置
     */
    public void clearAllActivities() {
        try {
            redisTemplate.delete(TABLE_NAME);
        } catch (Exception e) {
            log.error("清空活动配置失败", e);
        }
    }

}
