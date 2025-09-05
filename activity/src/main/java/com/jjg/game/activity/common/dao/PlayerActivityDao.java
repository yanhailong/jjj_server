package com.jjg.game.activity.common.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.common.redis.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class PlayerActivityDao {
    private final Logger log = LoggerFactory.getLogger(PlayerActivityDao.class);
    private final String TABLE_NAME = "activity:player:%s:%s";
    private final String LOCK_KEY = "activity:player:lock:%s:%s";
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public PlayerActivityDao(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, RedisLock redisLock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String getKey(long playerId, long activityType) {
        return String.format(TABLE_NAME, playerId, activityType);
    }

    public String getLockKey(long playerId, long activityId) {
        return String.format(LOCK_KEY, playerId, activityId);
    }


    /**
     * 获取单条
     */
    public <T> Map<Integer, T> getPlayerActivityData(long playerId, ActivityType activityType, long activityId) {
        try {
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            String jsonData = hash.get(getKey(playerId, activityType.getType()), String.valueOf(activityId));
            if (jsonData == null) {
                return null;
            }
            return objectMapper.readValue(jsonData, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("获取活动数据异常 playerId:{} activityType:{} activityId:{}",
                    playerId, activityType, activityId, e);
        }
        return null;
    }

    /**
     * 获取整组 Map
     */
    public <T> Map<Long, Map<Integer, T>> getAllPlayerActivityData(long playerId, ActivityType activityType) {
        try {
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            Map<String, String> entries = hash.entries(getKey(playerId, activityType.getType()));
            Map<Long, Map<Integer, T>> result = new HashMap<>();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                result.put(Long.parseLong(entry.getKey()), objectMapper.readValue(entry.getValue(),new TypeReference<>() {
                }));
            }
            return result;
        } catch (Exception e) {
            log.error("批量获取活动数据异常 playerId:{} activityType:{}",
                    playerId, activityType, e);
        }
        return new HashMap<>();
    }

    /**
     * 保存单条
     */
    public <T> void savePlayerActivityData(long playerId, ActivityType activityType, long activityId, T data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            hash.put(getKey(playerId, activityType.getType()), String.valueOf(activityId), json);
        } catch (Exception e) {
            log.error("保存单条活动数据异常 playerId:{} activityType:{} activityId:{}",
                    playerId, activityType, activityId, e);
        }
    }

    /**
     * 批量保存
     */
    public <T> void savePlayerActivityData(long playerId, ActivityType activityType, Map<Long, T> dataMap) {
        if (CollectionUtil.isEmpty(dataMap)) {
            return;
        }
        try {
            Map<String, String> hashMap = new HashMap<>();
            for (Map.Entry<Long, T> entry : dataMap.entrySet()) {
                hashMap.put(String.valueOf(entry.getKey()), objectMapper.writeValueAsString(entry.getValue()));
            }
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            hash.putAll(getKey(playerId, activityType.getType()), hashMap);
        } catch (Exception e) {
            log.error("批量保存活动数据异常 playerId:{} activityType:{} dataMapSize:{}",
                    playerId, activityType, dataMap.size(), e);
        }
    }

    /**
     * 删除单条
     */
    public void deletePlayerActivityData(long playerId, ActivityType activityType, long activityId) {
        try {
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            hash.delete(getKey(playerId, activityType.getType()), String.valueOf(activityId));
        } catch (Exception e) {
            log.error("删除活动数据异常 playerId:{} activityType:{} activityId:{}",
                    playerId, activityType, activityId, e);
        }
    }

    /**
     * 清空整组
     */
    public void clearPlayerActivityData(long playerId, ActivityType activityType) {
        try {
            redisTemplate.delete(getKey(playerId, activityType.getType()));
        } catch (Exception e) {
            log.error("清空活动数据异常 playerId:{} activityType:{}",
                    playerId, activityType, e);
        }
    }
}
