package com.jjg.game.hall.minigame.game.luckytreasure.dao;

import com.jjg.game.hall.minigame.game.luckytreasure.constant.LuckyTreasureConstant;
import com.jjg.game.hall.minigame.game.luckytreasure.data.LuckyTreasureConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 夺宝奇兵配置Redis操作类
 */
@Component
public class LuckyTreasureConfigRedisDao {

    private final RedisTemplate<String, Object> redisTemplate;

    public LuckyTreasureConfigRedisDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取redis中的配置列表
     */
    public List<LuckyTreasureConfig> getConfigList() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_CONFIG);
        return entries.values().stream().map(m -> (LuckyTreasureConfig) m).toList();
    }

    /**
     * 覆盖redis中的配置信息
     */
    public void replaceConfigMap(Map<Integer, LuckyTreasureConfig> configMap) {
        redisTemplate.opsForHash().putAll(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_CONFIG, configMap);
    }

    /**
     * 覆盖redis中的配置信息
     */
    public void replaceConfigMap(int id, LuckyTreasureConfig cfg) {
        redisTemplate.opsForHash().put(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_CONFIG, id, cfg);
    }

    /**
     * 删除Redis中存储的夺宝奇兵配置缓存。
     */
    public void deleteConfigMap() {
        redisTemplate.delete(LuckyTreasureConstant.RedisKey.LUCKY_TREASURE_CONFIG);
    }
}
