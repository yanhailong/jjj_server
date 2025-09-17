package com.jjg.game.activity.common.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.ObjectMapperUtil;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 活动详情DAO
 * <p>
 * 用途：
 *   - 管理 Redis 中的活动详情数据
 *   - 提供活动详情的增、删、改、查接口
 *   - 使用 Redis Hash 存储，每个活动对应一个 Hash 表
 *   - 使用分布式锁，避免并发操作时数据不一致
 * <p>
 * 说明：
 *   - Redis Hash 表名格式：activity:server:detail:{activityId}
 *   - Hash field = 详情ID (Integer)
 *   - Hash value = 详情数据（序列化后的 JSON 字符串）
 *
 * @author lm
 * @date 2025/9/4
 */
@Repository
public class ActivityDetailDao {
    private final Logger log = LoggerFactory.getLogger(ActivityDetailDao.class);

    /** Redis Hash 表名模板：按活动ID区分 */
    private final String TABLE_NAME = "activity:server:detail:%d";
    /** 单个详情数据的分布式锁模板：activity:detaillock:{activityId}:{detailId} */
    private final String ACTIVITY_DETAIL_LOCK = "activity:detaillock:%d:%d";
    /** 批量详情数据的全局锁模板：activity:alldetaillock:{activityId} */
    private final String ACTIVITY_ALL_DETAIL_LOCK = "activity:alldetaillock:%d";

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;
    /** Redis 操作模板，key=表名，field=详情ID，value=JSON字符串 */
    private final RedisTemplate<String, Integer> redisTemplate;
    /** 分布式锁工具 */
    private final RedisLock redisLock;

    public ActivityDetailDao(RedisTemplate<String, Integer> redisTemplate, RedisLock redisLock) {
        this.redisTemplate = redisTemplate;
        this.redisLock = redisLock;
        // 使用统一配置的 ObjectMapper
        objectMapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
    }

    /**
     * 获取单个详情锁Key
     * @param activityId 活动ID
     * @param detailId 详情ID
     */
    public String getLockKey(long activityId, int detailId) {
        return ACTIVITY_DETAIL_LOCK.formatted(activityId, detailId);
    }

    /**
     * 获取某个活动的全局详情锁Key（用于批量操作）
     * @param activityId 活动ID
     */
    public String getAllLockKey(long activityId) {
        return ACTIVITY_ALL_DETAIL_LOCK.formatted(activityId);
    }

    /**
     * 获取活动详情的 Hash 表名
     * @param activityId 活动ID
     */
    private String getKey(long activityId) {
        return String.format(TABLE_NAME, activityId);
    }

    /**
     * 获取 Redis Hash 操作对象
     * key=表名(String)，field=详情ID(Integer)，value=JSON字符串(String)
     */
    private HashOperations<String, Integer, String> opsForHash() {
        return redisTemplate.opsForHash();
    }

    /**
     * 获取某个活动的全部详情数据
     *
     * @param activityId 活动ID
     * @param activityType 活动类型（用于获取详情数据对应的Class类型）
     * @param <T> 详情数据类型（继承自 BaseCfgBean）
     * @return Map<详情ID, 详情对象>；失败时返回空Map
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseCfgBean> Map<Integer, T> getActivityDetailInfos(long activityId, ActivityType activityType) {
        try {
            // 获取 Redis 中所有详情配置
            Map<Integer, String> entries = opsForHash().entries(getKey(activityId));
            if (CollectionUtil.isEmpty(entries)) {
                return new HashMap<>();
            }

            // 获取具体活动类型对应的详情数据Class
            Class<T> detailDataClass = (Class<T>) activityType.getController().getDetailDataClass();
            Map<Integer, T> map = new HashMap<>(entries.size());

            // 反序列化 JSON -> 对象
            for (Map.Entry<Integer, String> entry : entries.entrySet()) {
                T obj = objectMapper.readValue(entry.getValue(), detailDataClass);
                map.put(entry.getKey(), obj);
            }
            return map;
        } catch (Exception e) {
            log.error("获取活动详情配置异常, activityId={}, type={}", activityId, activityType, e);
        }
        return Map.of();
    }

    /**
     * 获取某个活动的单个详情数据
     *
     * @param activityId 活动ID
     * @param activityType 活动类型（决定反序列化Class）
     * @param detailId 详情ID
     * @param <T> 详情数据类型
     * @return 详情数据对象；失败或不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseCfgBean> T getActivityDetail(long activityId, ActivityType activityType, String detailId) {
        try {
            // 从 Redis Hash 取值
            String value = opsForHash().get(getKey(activityId), detailId);
            if (value == null) {
                return null;
            }
            Class<T> detailDataClass = (Class<T>) activityType.getController().getDetailDataClass();
            return objectMapper.readValue(value, detailDataClass);
        } catch (Exception e) {
            log.error("获取活动详情失败 activityId={}, detailId={}", activityId, detailId, e);
        }
        return null;
    }

    /**
     * 保存或更新单个详情数据
     * 使用分布式锁保证并发安全
     *
     * @param activityId 活动ID
     * @param detailId 详情ID
     * @param detail 详情对象
     */
    public void saveActivityDetail(long activityId, int detailId, BaseCfgBean detail) {
        String lockKey = getLockKey(activityId, detailId);
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            // 序列化对象为 JSON 存入 Redis
            String json = objectMapper.writeValueAsString(detail);
            opsForHash().put(getKey(activityId), detailId, json);
        } catch (Exception e) {
            log.error("保存活动详情失败 activityId={}, detailId={}", activityId, detailId, e);
        } finally {
            redisLock.unlock(lockKey);
        }
    }

    /**
     * 批量保存活动详情
     * 使用活动全局锁，避免多个线程并发写入冲突
     *
     * @param activityId 活动ID
     * @param details Map<详情ID, 详情对象>
     */
    public void saveActivityDetails(long activityId, Map<Integer, BaseCfgBean> details) {
        if (CollectionUtil.isEmpty(details)) {
            return;
        }
        String allLockKey = getAllLockKey(activityId);
        redisLock.lock(allLockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            // 转换为 JSON Map
            Map<Integer, String> jsonMap = new HashMap<>();
            for (Map.Entry<Integer, BaseCfgBean> entry : details.entrySet()) {
                jsonMap.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            }
            opsForHash().putAll(getKey(activityId), jsonMap);
        } catch (Exception e) {
            log.error("批量保存活动详情失败 activityId={}, size={}", activityId, details.size(), e);
        } finally {
            redisLock.unlock(allLockKey);
        }
    }

    /**
     * 删除单个详情数据
     * @param activityId 活动ID
     * @param detailId 详情ID
     */
    public void deleteActivityDetail(long activityId, int detailId) {
        try {
            opsForHash().delete(getKey(activityId), detailId);
        } catch (Exception e) {
            log.error("删除活动详情失败 activityId={}, detailId={}", activityId, detailId, e);
        }
    }

    /**
     * 清空某个活动的全部详情
     * 删除整个 Hash 表
     * @param activityId 活动ID
     */
    public void clearActivityDetails(long activityId) {
        try {
            redisTemplate.delete(getKey(activityId));
        } catch (Exception e) {
            log.error("清空活动详情失败 activityId={}", activityId, e);
        }
    }
}
