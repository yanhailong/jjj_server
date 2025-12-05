package com.jjg.game.hall.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.ObjectMapperUtil;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.match.MatchDataDao;
import com.jjg.game.hall.dao.HallRoomDao;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

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
                -- HSETNX 如果字段不存在则设置，返回1；如果字段已存在，返回0
                local result = redis.call('HSETNX', KEYS[2], ARGV[2], ARGV[3])
                if result == 1 then
                    -- 计算新 score 并更新
                    local newScore = (1 * 4398046511104) + (1 * 4294967296) + seconds
                    redis.call('ZADD', KEYS[1], newScore, ARGV[2])
                    return tonumber(ARGV[2]);
                else
                    return 0;
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
        Pair<Long, String> preCreateData = getPreCreateRoomData(gameType, roomConfigId, maxPlayer, nodePath);
        if (preCreateData == null) {
            return 0;
        }
        Long result = redissonClient.getScript(LongCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE,
                        TRY_JOIN_ROOM_SCRIPT,
                        RScript.ReturnType.INTEGER,
                        List.of(matchRedisKey, hallRoomDao.getTableName(gameType)), // 传入两个 KEY
                        maxPlayer, preCreateData.getFirst(), preCreateData.getSecond());
        if (result == null) {
            return 0;
        }
        if (result.equals(preCreateData.getFirst())) {
            log.debug("创建房间是生成的房间id = {}", preCreateData.getFirst());
        }
        return result;
    }

    private Pair<Long, String> getPreCreateRoomData(int gameType, int roomConfigId, int maxPlayer, String nodePath) {
        Room preCreateRoom = hallRoomDao.preCreateRoom(gameType, roomConfigId, maxPlayer, nodePath);
        if (preCreateRoom == null) {
            log.error("getWaitingRoomId 预创建房间失败 gameType = {},roomConfigId = {} nodePath = {}", gameType, roomConfigId, nodePath);
            return null;
        }
        ObjectMapper mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
        String preCreateData = null;
        try {
            preCreateData = mapper.writeValueAsString(preCreateRoom);
        } catch (Exception e) {
            log.error("getWaitingRoomId 预处理房间失败", e);
        }
        return Pair.newPair(preCreateRoom.getId(), preCreateData);
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
