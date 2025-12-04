package com.jjg.game.hall.match;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.core.utils.RoomScoreUtil;
import com.jjg.game.hall.dao.HallRoomDao;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

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
    private static final String TRY_JOIN_ROOM_SCRIPT = """
            -- 获取分数最小的一个房间
            local entries = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
            if not entries or #entries == 0 then
                return 0
            end
            local roomId = entries[1]
            local score = tonumber(entries[2])
            
            -- 解析 score (参考 RoomScoreUtil)
            local seconds = score % 4294967296
            local rest = math.floor(score / 4294967296)
            local readyPlayers = rest % 1024
            local currentMaxPlayers = math.floor(rest / 1024)
            
            local maxLimit = tonumber(ARGV[1])
            
            -- 检查是否已满
            if currentMaxPlayers >= maxLimit then
                return 0
            end
            
            -- 更新人数 (+1)
            local newMax = currentMaxPlayers + 1
            local newReady = readyPlayers + 1
            
            -- 计算新 score 并更新
            local newScore = (newMax * 4398046511104) + (newReady * 4294967296) + seconds
            redis.call('ZADD', KEYS[1], newScore, roomId)
            
            return tonumber(roomId)
            """;
    public MatchService(MatchDataDao matchDataDao, RedisLock redisLock, HallRoomDao hallRoomDao, RedissonClient redissonClient) {
        this.matchDataDao = matchDataDao;
        this.redisLock = redisLock;
        this.hallRoomDao = hallRoomDao;
        this.redissonClient = redissonClient;
    }

    /**
     * 获取一个处于等待中的房间
     */
    public long getWaitingRoomId(int gameType, int roomConfigId, int maxPlayer, String nodePath) {
        String matchRedisKey = matchDataDao.getMatchRedisKey(gameType, roomConfigId);

        // 1. 尝试通过 Lua 脚本原子性地获取并加入现有房间
        // 如果成功，脚本内部会更新人数并返回 roomId
        Long joinedRoomId = redissonClient.getScript(LongCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE,
                        TRY_JOIN_ROOM_SCRIPT,
                        RScript.ReturnType.INTEGER,
                        Collections.singletonList(matchRedisKey),
                        maxPlayer);
        if (joinedRoomId != null && joinedRoomId > 0) {
            log.debug("加入现有房间 success roomId:{} gameType:{} roomConfigId:{}", joinedRoomId, gameType, roomConfigId);
            return joinedRoomId;
        }
        // 2. 如果没有可用房间或房间已满，则创建新房间
        // 这里移除了原有的分布式锁。虽然在极端高并发下可能导致瞬间创建多个房间，
        // 但这避免了锁带来的全局阻塞，通常是可以接受的。
        try {
            Room room = hallRoomDao.createRoom(gameType, roomConfigId, maxPlayer, nodePath);
            long waitingRoomId = room.getId();

            // 计算初始分值 (1人, 1准备)
            double score = RoomScoreUtil.computeScore(1, 1, (int) (System.currentTimeMillis() / 1000));
            log.debug("大厅创建新房间 gameType:{} roomConfigId:{} roomId:{}", gameType, roomConfigId, waitingRoomId);

            // 放入 Redis 等待列表
            RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(matchRedisKey,LongCodec.INSTANCE);
            scoredSortedSet.add(score, waitingRoomId);

            return waitingRoomId;
        } catch (Exception e) {
            log.error("getWaitingRoomId create room error", e);
            return 0;
        }
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
