package com.jjg.game.core.base.drop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 条件进度dao
 * 临时保存条件进度，比如某些累计
 *
 * @author 2CL
 */
@Repository
public class ConditionProgressDao {

    @Autowired
    private RedisTemplate<String, Number> redisTemplate;

    /**
     * 获取表名
     */
    private String getConditionProgressTableName() {
        // 条件表表key player玩家ID 条件进度key = 条件进度
        return "ConditionProgress";
    }

    /**
     * 添加条件进度
     */
    public void addProgress(String conditionKey, Number progress) {
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
}
