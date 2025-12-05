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
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                local acquired = redis.call('SET', createLockKey, '1', 'NX', 'EX', 3)
                if acquired then
                    -- 抢到了创建权，返回 -1 让 Java 代码去创建
                    return -1
                else
                    -- 没抢到，说明有人在创建，返回 0 让 Java 代码等待重试
                    return 0
                end
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
        String createLockKey = "lock:createToken:" + matchRedisKey; // 简单的 String Key
        // 循环重试
        for (int i = 0; i < 5; i++) {
            Long result = redissonClient.getScript(LongCodec.INSTANCE)
                    .eval(RScript.Mode.READ_WRITE,
                            TRY_JOIN_ROOM_SCRIPT,
                            RScript.ReturnType.INTEGER,
                            List.of(matchRedisKey, createLockKey), // 传入两个 KEY
                            maxPlayer);
            if (result != null) {
                // Case 1: 成功加入现有房间
                if (result > 0) {
                    return result;
                }
                // Case 2: 抢到了创建权 (-1)
                if (result == -1) {
                    try {
                        // 执行创建逻辑
                        Room room = hallRoomDao.createRoom(gameType, roomConfigId, maxPlayer, nodePath);
                        long waitingRoomId = room.getId();
                        // 初始分值 (1人, 1准备)
                        double score = RoomScoreUtil.computeScore(1, 1, (int) (System.currentTimeMillis() / 1000));
                        // 放入 Redis 等待列表
                        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(matchRedisKey, LongCodec.INSTANCE);
                        scoredSortedSet.add(score, waitingRoomId);
                        log.debug("创建新房间并加入成功 roomId:{}", waitingRoomId);
                        return waitingRoomId;
                    } catch (Exception e) {
                        log.error("创建房间失败", e);
                        return 0;
                    } finally {
                        // 创建完成后，一定要删除创建标记，让其他人知道创建结束了
                        // (虽然 Lua 设置了 TTL，但主动删除能让并发吞吐更高)
                        redissonClient.getBucket(createLockKey).delete();
                    }
                }
                // Case 3: result == 0，说明没抢到创建权，且没有房间
                // 意味着有人正在创建中。
                // 稍微睡一下，等待那个人的房间创建好
                try {
                    Thread.sleep(50 + (long) (Math.random() * 50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }
        }
        log.warn("重试多次仍未获取到房间 gameType:{} roomConfigId:{}", gameType, roomConfigId);
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
