package com.jjg.game.core.base.drop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.utils.ObjectMapperUtil;
import com.jjg.game.common.utils.TimeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;

/**
 * 道具掉落Dao
 * 1.临时保存条件进度，比如某些进度累计条件
 * 2.道具掉落分组次数记录
 *
 * @author 2CL
 */
@Repository
public class DropItemDao {

    private final Logger log = LoggerFactory.getLogger(DropItemDao.class);
    private final RedisTemplate<String, String> itemDropGroupMap;
    private final ObjectMapper objectMapper;
    // 道具掉落分组计数器，按天重置，玩家ID <=> 分组使用次数记录map
    private final String itemDropGroupCounter = "itemDropGroupCounter:";

    public DropItemDao(RedisTemplate<String, String> itemDropGroupMap) {
        this.itemDropGroupMap = itemDropGroupMap;
        this.objectMapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
    }


    private String getItemDropGroupCounterTableName() {
        int dayNumerical = TimeHelper.getDayNumerical();
        return itemDropGroupCounter + dayNumerical;
    }

    /**
     * 获取道具分组计数
     */
    public Map<Integer, Integer> getItemDropGroupCounter(long playerId) {
        String tableName = getItemDropGroupCounterTableName();
        HashOperations<String, String, String> opsForHash = itemDropGroupMap.opsForHash();
        String json = opsForHash.get(tableName, playerId);
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("json parse error", e);
        }
        return Map.of();
    }

    /**
     * 更新道具分组计数
     */
    public void updateItemDropGroupCounter(long playerId, Map<Integer, Integer> itemDropGroupCounter) {
        String tableName = getItemDropGroupCounterTableName();
        try {
            String json = objectMapper.writeValueAsString(itemDropGroupCounter);
            itemDropGroupMap.opsForHash().put(tableName, playerId, json);
            //设置2天过期
            itemDropGroupMap.expire(tableName, Duration.ofDays(2));
        } catch (Exception e) {
            log.error("json parse error playerId:{}", playerId, e);
        }
    }
}
