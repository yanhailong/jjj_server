package com.jjg.game.core.match;

import com.jjg.game.core.RedisLock;
import com.jjg.game.core.match.data.MatchDataRedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 匹配相关的数据查询逻辑
 *
 * @author 2CL
 */
@Component
public class MatchDataDao {

    @Autowired
    private RedisTemplate<String, String> matchKeyTemplate;
    @Autowired
    private RedisLock redisLock;

    public String getMatchRedisKey(int gameType, int roomConfigId) {
        return MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
    }

    public String getLockMatchRedisKey(int gameType, int roomConfigId) {
        return "lock:" + MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
    }

    /**
     * 获取正在等待加入的房间ID
     *
     * @return 获取到的等待房间ID
     */
    public long getWaitJoinRoomId(int gameType, int roomConfigId) {
        if (redisLock.tryLock(getLockMatchRedisKey(gameType, roomConfigId))) {
            try {
                Set<ZSetOperations.TypedTuple<String>> roomIds =
                    matchKeyTemplate.opsForZSet().rangeWithScores(MatchDataRedisKey.getWaitJoinRoomsKey(gameType,
                            roomConfigId)
                        , 0, 0);
                if (roomIds == null) {
                    return 0;
                }
                ZSetOperations.TypedTuple<String> typedTuple = roomIds.iterator().next();
                if (typedTuple == null || typedTuple.getValue() == null) {
                    return 0;
                }
                return roomIds.isEmpty() ? 0 : Long.parseLong(typedTuple.getValue());
            } finally {
                redisLock.tryUnlock(getLockMatchRedisKey(gameType, roomConfigId));
            }
        }
        return 0;
    }

    /**
     * 将房间ID从房间中移除
     */
    public boolean removeWaitJoinRoomId(int gameType, int roomConfigId, long roomId) {
        if (redisLock.tryLock(getLockMatchRedisKey(gameType, roomConfigId))) {
            try {
                String redisKey = MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
                matchKeyTemplate.opsForZSet().remove(redisKey, roomId);
                return true;
            } finally {
                redisLock.tryUnlock(getLockMatchRedisKey(gameType, roomConfigId));
            }
        }
        return false;
    }

    /**
     * 添加房间等待ID
     */
    public boolean addWaitJoinRoomId(int gameType, int roomConfigId, long roomId, long roomCreateTime) {
        if (redisLock.tryLock(getLockMatchRedisKey(gameType, roomConfigId))) {
            try {
                String redisKey = MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
                matchKeyTemplate.opsForZSet().add(redisKey, roomId + "", roomCreateTime);
                return true;
            } finally {
                redisLock.tryUnlock(getLockMatchRedisKey(gameType, roomConfigId));
            }
        }
        return false;
    }
}
