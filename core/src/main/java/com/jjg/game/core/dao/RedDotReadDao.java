package com.jjg.game.core.dao;

import com.jjg.game.common.redis.PlayerKeyIndex;
import com.jjg.game.common.utils.TimeHelper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.Set;

/** 展示用未读ID和每日查看日期，不修改业务奖励/成长状态。 */
@Repository
public class RedDotReadDao {
    private final RedissonClient redis;
    private final PlayerKeyIndex index;

    public RedDotReadDao(RedissonClient redis, PlayerKeyIndex index) {
        this.redis = redis;
        this.index = index;
    }

    private String key(long playerId, String scope) {
        return "red_dot_read:" + playerId + ":" + scope;
    }

    public Set<Integer> unread(long playerId, String scope) {
        return redis.<Integer>getSet(key(playerId, scope)).readAll();
    }

    public void addUnread(long playerId, String scope, Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        String key = key(playerId, scope);
        index.addSetKey(playerId, key);
        redis.<Integer>getSet(key).addAll(ids);
    }

    public void read(long playerId, String scope, Collection<Integer> ids) {
        if (ids != null && !ids.isEmpty()) redis.<Integer>getSet(key(playerId, scope)).removeAll(ids);
    }

    public boolean viewedToday(long playerId, String scope) {
        Integer day = redis.<String, Integer>getMap(key(playerId, "daily")).get(scope);
        return day != null && day == TimeHelper.getDayNumerical();
    }

    public void viewToday(long playerId, String scope) {
        String key = key(playerId, "daily");
        index.addHash(playerId, key, scope);
        redis.<String, Integer>getMap(key).put(scope, TimeHelper.getDayNumerical());
    }
}
