package com.jjg.game.hall.match;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.core.utils.RoomScoreUtil;
import com.jjg.game.hall.dao.HallRoomDao;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 预留逻辑
 * 房间匹配服务，通过等待房间ID是否存在，判断房间是否可以加入
 *
 * @author 2CL
 */
@Service
public class MatchService {

    private final Logger log = LoggerFactory.getLogger(MatchService.class);
    private final MatchDataDao matchDataDao;
    private final RedisLock redisLock;
    private final HallRoomDao hallRoomDao;
    private final RedissonClient redissonClient;

    public MatchService(MatchDataDao matchDataDao, RedisLock redisLock, HallRoomDao hallRoomDao, RedissonClient redissonClient) {
        this.matchDataDao = matchDataDao;
        this.redisLock = redisLock;
        this.hallRoomDao = hallRoomDao;
        this.redissonClient = redissonClient;
    }

    /**
     * 获取一个处于等待中的房间
     */
    public long getWaitingRoomId(PlayerController playerController, int gameType, int roomConfigId, int maxPlayer, String nodePath) {
        String lockMatchRedisKey = matchDataDao.getLockMatchRedisKey(gameType, roomConfigId);
        boolean locked = false;
        try {
            locked = redisLock.tryLock(lockMatchRedisKey, matchDataDao.MATCH_MAX_LOCK_HOLD_TIME);
            if (locked) {
                String matchRedisKey = matchDataDao.getMatchRedisKey(gameType, roomConfigId);
                RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(matchRedisKey);
                ScoredEntry<Long> first = scoredSortedSet.firstEntry();
                RoomScoreUtil.RoomScoreInfo roomScoreInfo = RoomScoreUtil.parseScore(first == null ? 0 : first.getScore());
                if (roomScoreInfo.seconds() == 0 || roomScoreInfo.maxPlayers() >= maxPlayer) {
                    //创建房间
                    Room room = hallRoomDao.createRoom(gameType, roomConfigId, maxPlayer, nodePath);
                    long waitingRoomId = room.getId();
                    //放入等待列表
                    double score = RoomScoreUtil.computeScore(1, 1, (int) (System.currentTimeMillis() / 1000));
                    log.debug("大厅创建新房间 gameType:{} roomConfigId:{} ", gameType, roomConfigId);
                    scoredSortedSet.add(score, waitingRoomId);
                    //返回房间id
                    return waitingRoomId;
                }
                if (first == null) {
                    return 0;
                }
                Long roomId = first.getValue();
                //更新人数
                Double score = scoredSortedSet.getScore(roomId);
                if (score == null) {
                    return 0;
                }
                int newReadyPlayer = roomScoreInfo.readyPlayers() + 1;
                int newMaxPlayer = roomScoreInfo.maxPlayers() + 1;
                double computed = RoomScoreUtil.computeScore(newMaxPlayer, newReadyPlayer, roomScoreInfo.seconds());
                scoredSortedSet.add(computed, roomId);
                log.debug("更新后房间缓存数据 roomId:{} gameType:{} roomConfigId:{} maxPlayer:{} readyPlayer:{}", roomId, gameType, roomConfigId, newMaxPlayer, newReadyPlayer);
                return roomId;
            }
        } catch (Exception e) {
            log.error("getWaitingRoomId ", e);
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockMatchRedisKey);
            }
        }
        return 0;
    }

    /**
     * 添加到等待房间ID
     */
    public void addWaitingRoomId(int gameType, int roomConfigId, long roomId, long roomCreateTime) {
        matchDataDao.addWaitJoinRoomId(gameType, roomConfigId, roomId, roomCreateTime);
    }

    /**
     * 添加玩家过期等待
     */
    public void addPlayerExpiredWaiting(long roomId, long playerId) {
        matchDataDao.addPlayerExpiredWaiting(roomId, playerId);
    }
}
