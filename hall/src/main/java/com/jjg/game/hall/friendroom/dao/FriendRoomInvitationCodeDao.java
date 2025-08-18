package com.jjg.game.hall.friendroom.dao;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.jjg.game.common.utils.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 好友房邀请码dao,重置次数记录
 *
 * @author 2CL
 */
@Repository
public class FriendRoomInvitationCodeDao {

    /**
     * table_name TableName + "按天的时间" 玩家ID + 邀请码重置次数
     */
    private static final String TABLE_NAME = "FriendRoomInvitationCode";

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    // 表名缓存，防止重复判断。如果是重新开服，此值会清空，但是也只是让key值多存一点时间，不影响其他逻辑
    private final Set<Integer> tableNameCache = new ConcurrentHashSet<>();

    private String getTableName() {
        int dayNumerical = TimeHelper.getDayNumerical();
        String curDayKey = TABLE_NAME + dayNumerical;
        if (!tableNameCache.contains(dayNumerical)) {
            tableNameCache.add(dayNumerical);
            if (!redisTemplate.hasKey(curDayKey)) {
                // 设置过期时间
                redisTemplate.expire(
                    curDayKey, TimeHelper.ONE_DAY_OF_MILLIS + TimeHelper.ONE_MINUTE_OF_MILLIS, TimeUnit.MILLISECONDS);
            }
        }
        return curDayKey;
    }

    /**
     * 改变重置次数
     */
    public long addUseTimes(long playerId) {
        String tableName = getTableName();
        return redisTemplate.opsForHash().increment(tableName, playerId, 1);
    }

    /**
     * 获取玩家重置次数
     */
    public Integer getUseTimes(long playerId) {
        String tableName = getTableName();
        return (Integer) redisTemplate.opsForHash().get(tableName, playerId);
    }
}
