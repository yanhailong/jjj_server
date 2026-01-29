package com.jjg.game.activity.common.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.common.redis.PlayerRedis;
import com.jjg.game.common.utils.ObjectMapperUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 玩家活动数据 DAO
 * <p>
 * 主要负责玩家活动数据在 Redis 中的存取操作，包括：
 * - 单条/批量保存
 * - 单条/批量获取
 * - 删除与清空
 * - Redis 分布式锁 key 生成
 * <p>
 * 数据存储结构：
 * Redis Hash：
 * key   = activity:player:{playerId}:{activityType}
 * field = activityId
 * value = 活动数据的 JSON 字符串
 *
 * @author lm
 */
@Repository
public class PlayerActivityDao {
    private final Logger log = LoggerFactory.getLogger(PlayerActivityDao.class);
    /**
     * Redis 存储 key 模板
     * %s = playerId, activityType
     */
    private final String TABLE_NAME = "activity:player:%s:%s";

    /**
     * Redis 分布式锁 key 模板
     * %s = playerId, activityId
     */
    private final String LOCK_KEY = "activity:player:lock:%s:%s";

    private final RedisTemplate<String, String> redisTemplate;
    private final PlayerRedis playerRedis;
    private final ObjectMapper mapper;

    public PlayerActivityDao(RedisTemplate<String, String> redisTemplate, PlayerRedis playerRedis) {
        this.redisTemplate = redisTemplate;
        this.playerRedis = playerRedis;
        this.mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
    }

    // -------------------- Key 构造方法 --------------------

    /**
     * 获取 Redis 存储 key
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     */
    private String getKey(long playerId, long activityType) {
        return String.format(TABLE_NAME, playerId, activityType);
    }

    /**
     * 获取 Redis 分布式锁 key
     *
     * @param playerId   玩家 ID
     * @param activityId 活动 ID
     */
    public String getLockKey(long playerId, long activityId) {
        return String.format(LOCK_KEY, playerId, activityId);
    }

    // -------------------- 查询方法 --------------------

    /**
     * 获取玩家单个活动的数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param activityId   活动 ID
     * @param <T>          活动数据类型（必须继承 PlayerActivityData）
     * @return key 为子类型 ID，value 为具体数据对象；若无数据则返回空 Map
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
            return new HashMap<>();
        }
    }

    /**
     * 获取玩家某一类活动的所有数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param <T>          活动数据类型
     * @return key 为活动 ID，value 为该活动下的数据 Map；若无数据则返回空 Map
     */
    public <T extends PlayerActivityData> Map<Long, Map<Integer, T>> getAllPlayerActivityData(
            long playerId, ActivityType activityType) {
        try {
            HashOperations<String, String, String> hash = redisTemplate.opsForHash();
            Map<String, String> entries = hash.entries(getKey(playerId, activityType.getType()));
            if (CollectionUtil.isEmpty(entries)) {
                return new HashMap<>();
            }

            Map<Long, Map<Integer, T>> result = new HashMap<>(entries.size());
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                result.put(Long.parseLong(entry.getKey()),
                        mapper.readValue(entry.getValue(), new TypeReference<>() {
                        }));
            }
            return result;
        } catch (Exception e) {
            log.error("批量获取活动数据异常 playerId:{} activityType:{}",
                    playerId, activityType, e);
            return new HashMap<>();
        }
    }

    // -------------------- 保存方法 --------------------

    /**
     * 保存玩家的单条活动数据
     *
     * @param playerId     玩家 ID
     * @param activityType 活动类型
     * @param activityId   活动 ID
     * @param data         活动数据 Map
     */
    public <T extends PlayerActivityData> void savePlayerActivityData(
            long playerId, ActivityType activityType, long activityId, Map<Integer, T> data) {
        try {
            String json = mapper.writeValueAsString(data);
            playerRedis.hset(
                    playerId,
                    getKey(playerId, activityType.getType()),
                    String.valueOf(activityId),
                    json
            );
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
     * @param dataMap      活动数据 Map，key 为活动 ID，value 为数据对象
     */
    public <T extends PlayerActivityData> void savePlayerActivityData(
            long playerId, ActivityType activityType, Map<Integer, T> dataMap) {
        if (CollectionUtil.isEmpty(dataMap)) {
            return;
        }
        try {
            Map<String, String> hashMap = new HashMap<>(dataMap.size());
            for (Map.Entry<Integer, T> entry : dataMap.entrySet()) {
                hashMap.put(String.valueOf(entry.getKey()), mapper.writeValueAsString(entry.getValue()));
            }
            playerRedis.hsetAll(playerId, getKey(playerId, activityType.getType()), hashMap);
        } catch (Exception e) {
            log.error("批量保存活动数据异常 playerId:{} activityType:{} dataMapSize:{}",
                    playerId, activityType, dataMap.size(), e);
        }
    }

    // -------------------- 删除方法 --------------------

    /**
     * 删除玩家的单条活动数据
     */
    public void deletePlayerActivityData(long playerId, ActivityType activityType, long activityId) {
        try {
            playerRedis.hdelete(playerId, getKey(playerId, activityType.getType()), String.valueOf(activityId));
        } catch (Exception e) {
            log.error("删除活动数据异常 playerId:{} activityType:{} activityId:{}",
                    playerId, activityType, activityId, e);
        }
    }

    /**
     * 清空玩家某一类活动的所有数据
     */
    public void clearPlayerActivityData(long playerId, ActivityType activityType) {
        try {
            playerRedis.hdeleteAll(playerId, getKey(playerId, activityType.getType()));
        } catch (Exception e) {
            log.error("清空活动数据异常 playerId:{} activityType:{}",
                    playerId, activityType, e);
        }
    }

    // -------------------- 首次登录触发时间--------------------

    /**
     * 检查是否能触发首次登录 保存10s
     * @param playerId 玩家id
     * @return true能触发 false不能触发
     */
    public Boolean checkCanTargetFirstLogin(long playerId) {
        return redisTemplate.opsForValue().setIfAbsent("acitvity:login:%d".formatted(playerId)
                , "1", 10, TimeUnit.SECONDS);
    }

}
