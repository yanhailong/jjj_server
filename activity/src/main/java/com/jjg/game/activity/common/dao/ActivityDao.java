package com.jjg.game.activity.common.dao;

import com.jjg.game.activity.common.data.ActivityData;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存活动运行快照，配置删除后仍可在重启时执行原活动的结束流程。
 */
@Repository
public class ActivityDao {
    private static final String TABLE_NAME = "activity:data";
    private final HashOperations<String, String, ActivityData> hash;

    public ActivityDao(RedisTemplate<String, ActivityData> redisTemplate) {
        this.hash = redisTemplate.opsForHash();
    }

    public List<ActivityData> getAllActivityData() {
        return hash.values(TABLE_NAME);
    }

    public void saveActivityData(ActivityData data) {
        hash.put(TABLE_NAME, String.valueOf(data.getId()), data);
    }

    public void saveActivityData(Collection<ActivityData> dataList) {
        Map<String, ActivityData> dataMap = new HashMap<>();
        for (ActivityData data : dataList) {
            dataMap.put(String.valueOf(data.getId()), data);
        }
        if (!dataMap.isEmpty()) {
            hash.putAll(TABLE_NAME, dataMap);
        }
    }
}
