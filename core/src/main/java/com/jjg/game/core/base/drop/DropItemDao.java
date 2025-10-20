package com.jjg.game.core.base.drop;

import com.jjg.game.common.utils.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
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
    private RedisTemplate<String, Map<Integer, Integer>> itemDropGroupMap;

    // 道具掉落分组计数器，按天重置，玩家ID <=> 分组使用次数记录map
    private final String itemDropGroupCounter = "itemDropGroupCounter";


    private String getItemDropGroupCounterTableName() {
        int dayNumerical = TimeHelper.getDayNumerical();
        return itemDropGroupCounter + dayNumerical;
    }

    /**
     * 获取道具分组计数
     */
    public Map<Integer, Integer> getItemDropGroupCounter(long playerId) {
        String tableName = getItemDropGroupCounterTableName();
        HashOperations<String, Object, Map<Integer, Integer>> opsForHash = itemDropGroupMap.opsForHash();
        return opsForHash.get(tableName, playerId);
    }

    /**
     * 更新道具分组计数
     */
    public void updateItemDropGroupCounter(long playerId, Map<Integer, Integer> itemDropGroupCounter) {
        String tableName = getItemDropGroupCounterTableName();
        itemDropGroupMap.opsForHash().put(tableName, playerId, itemDropGroupCounter);
    }
}
