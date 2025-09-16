package com.jjg.game.activity.common.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.common.utils.ObjectMapperUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家活动数据访问层
 *
 * <p>主要负责将玩家的活动数据存储到 Redis，以及从 Redis 中读取和删除。</p>
 * <p>数据存储结构：
 * <ul>
 *   <li>Redis Key: {@code activity:player:{playerId}:{activityType}}</li>
 *   <li>Field: {@code activityId}</li>
 *   <li>Value: 活动数据的 JSON 串</li>
 * </ul>
 * </p>
 *
 * <p>例如：
 * <pre>
 * Key = activity:player:1001:1
 * Field = 20001
 * Value = { ... 活动数据JSON ... }
 * </pre>
 * </p>
 *
 * @author
 */
@Repository
public class PlayerActivityDao {
    private final Logger log = LoggerFactory.getLogger(PlayerActivityDao.class);

    /**
     * Redis 存储 key 模板
     */
    private final String TABLE_NAME = "activity:player:%s:%s";
    /**
     * Redis 分布式锁 key 模板
     */
    private final String LOCK_KEY = "activity:player:lock:%s:%s";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper;

    public PlayerActivityDao(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
    }

    /**
     * 生成玩家活动数据的 Redis key
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @return Redis key
     */
    private String getKey(long playerId, long activityType) {
        return String.format(TABLE_NAME, playerId, activityType);
    }

    /**
     * 生成分布式锁 key
     *
     * @param playerId   玩家 ID
     * @param activityId 活动 ID
     * @return Redis 锁 key
     */
    public String getLockKey(long playerId, long activityId) {
        return String.format(LOCK_KEY, playerId, activityId);
    }

    /**
     * 获取玩家的单个活动数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param activityId   活动 ID
     * @param <T>          活动数据类型（必须继承 {@link PlayerActivityData}）
     * @return 活动数据 Map，key 为数据的子类型 ID，value 为具体数据；如果不存在返回空 Map
     */
    public <T extends PlayerActivityData> Map<Integer, T> getPlayerActivityData(
        long playerId, ActivityType activityType, long activityId) {
        try {
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            String jsonData = hash.get(getKey(playerId, activityType.getType()), String.valueOf(activityId));
            if (jsonData == null) {
                return new HashMap<>();
            }
            return mapper.readValue(jsonData, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("获取活动数据异常 playerId:{} activityType:{} activityId:{}",
                playerId, activityType, activityId, e);
        }
        return new HashMap<>();
    }

    /**
     * 获取玩家某一类活动的所有数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param <T>          活动数据类型（必须继承 {@link PlayerActivityData}）
     * @return 活动数据 Map，key 为活动 ID，value 为该活动下的数据 Map；如果不存在返回空 Map
     */
    public <T extends PlayerActivityData> Map<Long, Map<Integer, T>> getAllPlayerActivityData(long playerId,
                                                                                              ActivityType activityType) {
        try {
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            Map<String, String> entries = hash.entries(getKey(playerId, activityType.getType()));
            Map<Long, Map<Integer, T>> result = new HashMap<>();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                result.put(Long.parseLong(entry.getKey()), mapper.readValue(entry.getValue(), new TypeReference<>() {
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
     * 保存玩家的单条活动数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param activityId   活动 ID
     * @param data         活动数据 Map
     * @param <T>          活动数据类型（必须继承 {@link PlayerActivityData}）
     */
    public <T extends PlayerActivityData> void savePlayerActivityData(long playerId, ActivityType activityType,
                                                                      long activityId, Map<Integer, T> data) {
        try {
            String json = mapper.writeValueAsString(data);
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            hash.put(getKey(playerId, activityType.getType()), String.valueOf(activityId), json);
        } catch (Exception e) {
            log.error("保存单条活动数据异常 playerId:{} activityType:{} activityId:{}",
                playerId, activityType, activityId, e);
        }
    }

    /**
     * 批量保存玩家活动数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param dataMap      活动数据 Map，key 为活动 ID，value 为具体活动数据对象
     * @param <T>          活动数据类型
     */
    public <T> void savePlayerActivityData(long playerId, ActivityType activityType, Map<Integer, T> dataMap) {
        if (CollectionUtil.isEmpty(dataMap)) {
            return;
        }
        try {
            Map<String, String> hashMap = new HashMap<>();
            for (Map.Entry<Integer, T> entry : dataMap.entrySet()) {
                hashMap.put(String.valueOf(entry.getKey()), mapper.writeValueAsString(entry.getValue()));
            }
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            hash.putAll(getKey(playerId, activityType.getType()), hashMap);
        } catch (Exception e) {
            log.error("批量保存活动数据异常 playerId:{} activityType:{} dataMapSize:{}",
                playerId, activityType, dataMap.size(), e);
        }
    }

    /**
     * 删除玩家的单条活动数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param activityId   活动 ID
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
     * 清空玩家某一类活动的所有数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
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
