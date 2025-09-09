package com.jjg.game.activity.common.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.common.data.ActivityType;
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
 * 活动配置详情DAO
 * 用于操作 Redis 中的活动配置详情表：activity:server:detail:{activityId}
 * 每个活动ID对应一个 Hash，field 为子项ID，value 为 JSON
 *
 * @author lm
 * @date 2025/9/4
 */
@Repository
public class ActivityDetailDao {
    private static final Logger log = LoggerFactory.getLogger(ActivityDetailDao.class);
    private static final String TABLE_NAME = "activity:server:detail:%s";

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Integer> redisTemplate;

    public ActivityDetailDao(RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
        objectMapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
    }

    private String getKey(long activityId) {
        return String.format(TABLE_NAME, activityId);
    }

    private HashOperations<String, Integer, String> opsForHash() {
        return redisTemplate.opsForHash();
    }

    /**
     * 获取活动的所有详情数据
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseCfgBean> Map<Integer, T> getActivityDetailInfos(long activityId, ActivityType activityType) {
        try {
            Map<Integer, String> entries = opsForHash().entries(getKey(activityId));
            if (CollectionUtil.isEmpty(entries)) {
                return Map.of();
            }

            Class<T> detailDataClass = (Class<T>) activityType.getController().getDetailDataClass();
            Map<Integer, T> map = new HashMap<>(entries.size());

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
     * 获取单个详情
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseCfgBean> T getActivityDetail(long activityId, ActivityType activityType, String detailId) {
        try {
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
     * 保存或更新单个详情
     */
    public void saveActivityDetail(long activityId, int detailId, BaseCfgBean detail) {
        try {
            String json = objectMapper.writeValueAsString(detail);
            opsForHash().put(getKey(activityId), detailId, json);
        } catch (Exception e) {
            log.error("保存活动详情失败 activityId={}, detailId={}", activityId, detailId, e);
        }
    }

    /**
     * 批量保存活动详情
     */
    public void saveActivityDetails(long activityId, Map<Integer, BaseCfgBean> details) {
        try {
            if (CollectionUtil.isEmpty(details)) {
                return;
            }
            Map<Integer, String> jsonMap = new HashMap<>();
            for (Map.Entry<Integer, BaseCfgBean> entry : details.entrySet()) {
                jsonMap.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            }
            opsForHash().putAll(getKey(activityId), jsonMap);
        } catch (Exception e) {
            log.error("批量保存活动详情失败 activityId={}, size={}", activityId, details != null ? details.size() : 0, e);
        }
    }

    /**
     * 删除单个详情
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
     */
    public void clearActivityDetails(long activityId) {
        try {
            redisTemplate.delete(getKey(activityId));
        } catch (Exception e) {
            log.error("清空活动详情失败 activityId={}", activityId, e);
        }
    }
}
