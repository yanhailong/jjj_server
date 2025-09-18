package com.jjg.game.core.base.drop;

import com.jjg.game.common.utils.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

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

    @Autowired
    private RedisTemplate<String, Number> redisTemplate;

    @Autowired
    private RedisTemplate<String, Map<Integer, Integer>> itemDropGrouMap;

    // 道具掉落分组计数器，按天重置，玩家ID <=> 分组使用次数记录map
    private final String itemDropGroupCounter = "itemDropGroupCounter";

    /**
     * 获取表名
     */
    private String getConditionProgressTableName() {
        // 条件表表key player玩家ID 条件进度key = 条件进度
        return "ConditionProgress";
    }

    private String getItemDropGroupCounterTableName() {
        int dayNumerical = TimeHelper.getDayNumerical();
        return itemDropGroupCounter + dayNumerical;
    }

    /**
     * 添加条件进度
     */
    public void updateProgress(String conditionKey, Number progress) {
        String tableName = getConditionProgressTableName();
        redisTemplate.opsForHash().put(tableName, conditionKey, progress);
    }

    /**
     * 清理进度
     */
    public void clearProgress(String conditionKey) {
        String tableName = getConditionProgressTableName();
        redisTemplate.opsForHash().delete(tableName, conditionKey);
    }

    /**
     * 获取进度
     */
    public Number getProgress(String conditionKey) {
        String tableName = getConditionProgressTableName();
        return (Number) redisTemplate.opsForHash().get(tableName, conditionKey);
    }

    /**
     * 获取道具分组计数
     */
    public Map<Integer, Integer> getItemDropGroupCounter(long playerId) {
        String tableName = getItemDropGroupCounterTableName();
        return (Map<Integer, Integer>) itemDropGrouMap.opsForHash().get(tableName, playerId);
    }

    /**
     * 更新道具分组计数
     */
    public void updateItemDropGroupCounter(long playerId, Map<Integer, Integer> itemDropGroupCounter) {
        String tableName = getItemDropGroupCounterTableName();
        itemDropGrouMap.opsForHash().put(tableName, playerId, itemDropGroupCounter);
    }
}
