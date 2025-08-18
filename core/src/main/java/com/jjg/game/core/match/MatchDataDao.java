package com.jjg.game.core.match;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
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
    // 匹配逻辑最大锁持有时间
    private static final int MATCH_MAX_LOCK_HOLD_TIME = 1;

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
        try {
            if (redisLock.tryLock(getLockMatchRedisKey(gameType, roomConfigId), TimeHelper.ONE_SECOND_OF_MILES)) {
                Set<ZSetOperations.TypedTuple<String>> roomIds =
                    matchKeyTemplate.opsForZSet().rangeWithScores(MatchDataRedisKey.getWaitJoinRoomsKey(gameType,
                            roomConfigId)
                        , -1, -1);
                if (roomIds == null || roomIds.isEmpty()) {
                    return 0;
                }
                ZSetOperations.TypedTuple<String> typedTuple = roomIds.iterator().next();
                if (typedTuple == null || typedTuple.getValue() == null) {
                    return 0;
                }
                return roomIds.isEmpty() ? 0 : Long.parseLong(typedTuple.getValue());
            }
        } catch (InterruptedException ignored) {
        } finally {
            redisLock.tryUnlock(getLockMatchRedisKey(gameType, roomConfigId));
        }
        return 0;
    }

    /**
     * 获取正在等待加入的房间ID(排除老的房间id)
     *
     * @return 获取到的等待房间ID
     */
    public long getNewWaitJoinRoomId(int gameType, int roomConfigId, long oldRoomId) {
        try {
            if (redisLock.tryLock(getLockMatchRedisKey(gameType, roomConfigId), TimeHelper.ONE_SECOND_OF_MILES)) {
                Set<ZSetOperations.TypedTuple<String>> roomIds =
                    matchKeyTemplate.opsForZSet().rangeWithScores(MatchDataRedisKey.getWaitJoinRoomsKey(gameType,
                            roomConfigId)
                        , -1, -1);
                if (roomIds == null || roomIds.isEmpty()) {
                    return 0;
                }
                for (ZSetOperations.TypedTuple<String> typedTuple : roomIds) {
                    if (typedTuple == null || typedTuple.getValue() == null) {
                        continue;
                    }
                    long newRoomId = Long.parseLong(typedTuple.getValue());
                    if (newRoomId != oldRoomId) {
                        return newRoomId;
                    }
                }
            }
        } catch (InterruptedException ignored) {
        } finally {
            redisLock.tryUnlock(getLockMatchRedisKey(gameType, roomConfigId));
        }
        return 0;
    }

    /**
     * 将房间ID从房间中移除
     */
    public boolean removeWaitJoinRoomId(int gameType, int roomConfigId, long roomId) {
        redisLock.lock(getLockMatchRedisKey(gameType, roomConfigId), MATCH_MAX_LOCK_HOLD_TIME);
        try {
            String redisKey = MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
            matchKeyTemplate.opsForZSet().remove(redisKey, String.valueOf(roomId));
            return true;
        } finally {
            redisLock.unlock(getLockMatchRedisKey(gameType, roomConfigId));
        }
    }

    /**
     * 添加房间等待ID
     */
    public boolean addWaitJoinRoomId(int gameType, int roomConfigId, long roomId, long roomCreateTime) {
        // 最长等待5s
        redisLock.lock(getLockMatchRedisKey(gameType, roomConfigId), MATCH_MAX_LOCK_HOLD_TIME);
        try {
            String redisKey = MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
            matchKeyTemplate.opsForZSet().add(redisKey, roomId + "", roomCreateTime);
            return true;
        } finally {
            redisLock.unlock(getLockMatchRedisKey(gameType, roomConfigId));
        }
    }


    /**
     * 房间等待ID设置为-1将其移动到最前面
     */
    public boolean moveWaitJoinRoomIdToLast(int gameType, int roomConfigId, long roomId) {
        redisLock.lock(getLockMatchRedisKey(gameType, roomConfigId), MATCH_MAX_LOCK_HOLD_TIME);
        try {
            String redisKey = MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
            matchKeyTemplate.opsForZSet().add(redisKey, roomId + "", -1);
            return true;
        } finally {
            redisLock.tryUnlock(getLockMatchRedisKey(gameType, roomConfigId));
        }
    }
}
