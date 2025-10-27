package com.jjg.game.core.match;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.redis.RedissonLock;
import com.jjg.game.core.match.data.MatchDataRedisKey;
import com.jjg.game.core.utils.RoomScoreUtil;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 匹配相关的数据查询逻辑
 *
 * @author 2CL
 */
@Component
public class MatchDataDao {

    private final Logger log = LoggerFactory.getLogger(MatchDataDao.class);
    private final RedissonClient redissonClient;
    // 匹配逻辑最大锁持有时间
    public final int MATCH_MAX_LOCK_HOLD_TIME = 500;
    // 玩家等待key
    private final String PLAYER_WAIT_KEY = "match:playerWait:%s";
    private final RedisLock redisLock;

    public MatchDataDao(RedissonClient redissonClient, RedisLock redisLock) {
        this.redissonClient = redissonClient;
        this.redisLock = redisLock;
    }

    public String getPlayerWaitKey(long roomId) {
        return PLAYER_WAIT_KEY.formatted(roomId);
    }

    public String getMatchRedisKey(int gameType, int roomConfigId) {
        return MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
    }

    public String getLockMatchRedisKey(int gameType, int roomConfigId) {
        return "lock:" + MatchDataRedisKey.getWaitJoinRoomsKey(gameType, roomConfigId);
    }

    /**
     * 获取正在等待加入的房间ID(排除老的房间id)
     *
     * @return 获取到的等待房间ID
     */
    @RedissonLock(key = "#root.getLockMatchRedisKey(#gameType, #roomConfigId)", waitTime = MATCH_MAX_LOCK_HOLD_TIME)
    public long getNewWaitJoinRoomId(@Param("gameType") int gameType, @Param("roomConfigId") int roomConfigId, int maxLimit, long oldRoomId) {
        String matchRedisKey = getMatchRedisKey(gameType, roomConfigId);
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(matchRedisKey);
        Collection<ScoredEntry<Long>> scoredEntries = scoredSortedSet.entryRange(0, -1);
        for (ScoredEntry<Long> scoredEntry : scoredEntries) {
            RoomScoreUtil.RoomScoreInfo roomScoreInfo = RoomScoreUtil.parseScore(scoredEntry.getScore());
            if (roomScoreInfo.maxPlayers() >= maxLimit || scoredEntry.getValue().equals(oldRoomId)) {
                continue;
            }
            return scoredEntry.getValue();
        }
        return 0;
    }

    /**
     * 将房间ID从房间中移除
     */
    @RedissonLock(key = "#root.getLockMatchRedisKey(#gameType, #roomConfigId)", waitTime = MATCH_MAX_LOCK_HOLD_TIME)
    public boolean removeWaitJoinRoomId(@Param("gameType") int gameType, @Param("roomConfigId") int roomConfigId, long roomId) {
        String redisKey = getMatchRedisKey(gameType, roomConfigId);
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(redisKey);
        scoredSortedSet.remove(roomId);
        return true;
    }

    /**
     * 添加房间等待ID
     */
    @RedissonLock(key = "#root.getLockMatchRedisKey(#gameType, #roomConfigId)", waitTime = MATCH_MAX_LOCK_HOLD_TIME)
    public boolean addWaitJoinRoomId(@Param("gameType") int gameType, @Param("roomConfigId") int roomConfigId,
                                     long roomId, long roomCreateTime) {
        String redisKey = getMatchRedisKey(gameType, roomConfigId);
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(redisKey);
        double score = RoomScoreUtil.computeScore(0, 0, (int) (roomCreateTime / 1000));
        scoredSortedSet.add(score, roomId);
        return true;
    }


    /**
     * 改变房间加入人数
     *
     * @param gameType     游戏类型
     * @param roomConfigId 房间配置id
     * @param roomId       房间id
     * @param maxPlayerNum 房间最大人数限制
     * @param totalNum     改变的总人数
     * @param waitNum      改变的等待人数
     * @return 是否改变成功
     */
    public boolean changeRoomJoinNum(int gameType, int roomConfigId, long roomId, int maxPlayerNum, int totalNum, int waitNum) {
        String lockMatchRedisKey = getLockMatchRedisKey(gameType, roomConfigId);
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockMatchRedisKey, MATCH_MAX_LOCK_HOLD_TIME);
            if (locked) {
                String redisKey = getMatchRedisKey(gameType, roomConfigId);
                RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(redisKey);
                Double score = scoredSortedSet.getScore(roomId);
                if (score == null) {
                    return false;
                }
                RoomScoreUtil.RoomScoreInfo roomScoreInfo = RoomScoreUtil.parseScore(score);
                if (totalNum > 0 && (totalNum + roomScoreInfo.maxPlayers()) > maxPlayerNum ||
                        totalNum < 0 && roomScoreInfo.maxPlayers() + totalNum < 0) {
                    return false;
                }
                double computed = RoomScoreUtil.computeScore(Math.max(0, roomScoreInfo.maxPlayers() + totalNum),
                        Math.max(0, roomScoreInfo.readyPlayers() + waitNum), roomScoreInfo.seconds());
                scoredSortedSet.add(computed, roomId);
                return true;
            }
        } catch (Exception e) {
            log.error("changeRoomJoinNum ", e);
        } finally {
            if (locked) {
                redisLock.unlock(lockMatchRedisKey);
            }
        }
        return false;
    }

    /**
     * 添加玩家进入房间时的过期等待
     *
     * @param roomId   房间id
     * @param playerId 玩家id
     */
    public void addPlayerExpiredWaiting(long roomId, long playerId) {
        String playerWaitKey = getPlayerWaitKey(roomId);
        RSetCache<Long> setCache = redissonClient.getSetCache(playerWaitKey);
        setCache.remove(playerId); // 可选，确保刷新 TTL
        setCache.add(playerId, 10, TimeUnit.SECONDS);
    }

    /**
     * 添加获取玩家的等待人数
     *
     * @param roomId 房间id
     */
    public int getPlayerExpiredWaitingNum(long roomId) {
        String playerWaitKey = getPlayerWaitKey(roomId);
        return redissonClient.getSetCache(playerWaitKey).size();
    }

    /**
     * 检查玩家等待数据
     *
     * @param gameType     游戏类型
     * @param roomConfigId 房间配置id
     * @param roomId       房间id
     */
    public void checkPlayerExpiredWaitingNum(int gameType, int roomConfigId, long roomId) {
        String matchRedisKey = getMatchRedisKey(gameType, roomConfigId);
        String lockMatchRedisKey = getLockMatchRedisKey(gameType, roomConfigId);
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockMatchRedisKey, MATCH_MAX_LOCK_HOLD_TIME);
            if (locked) {
                //获取等待人数
                RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(matchRedisKey);
                Double score = scoredSortedSet.getScore(roomId);
                if (score == null) {
                    return;
                }
                RoomScoreUtil.RoomScoreInfo roomScoreInfo = RoomScoreUtil.parseScore(score);
                int readyPlayers = roomScoreInfo.readyPlayers();
                //获取过期等待人数
                int waitingNum = getPlayerExpiredWaitingNum(roomId);
                int more = readyPlayers - waitingNum;
                if (more > 0 && roomScoreInfo.maxPlayers() > more) {
                    //设置新的
                    double computed = RoomScoreUtil.computeScore(roomScoreInfo.maxPlayers() - more, roomScoreInfo.readyPlayers() - more, roomScoreInfo.seconds());
                    scoredSortedSet.add(computed, roomId);
                    log.info("更新房间等待人数 roomId:{} 减少人数:{}", roomId, more);
                }
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockMatchRedisKey);
            }
        }
    }
}
