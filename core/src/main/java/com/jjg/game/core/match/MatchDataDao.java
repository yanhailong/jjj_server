package com.jjg.game.core.match;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.match.data.MatchDataRedisKey;
import com.jjg.game.core.utils.RoomScoreUtil;
import org.redisson.api.RMapCache;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
    // 玩家等待key
    private final String PLAYER_WAIT_KEY = "match:playerWait:%s";
    private final RedisLock redisLock;
    // 解析规则参考 RoomScoreUtil: maxPlayers(10bit) | readyPlayers(10bit) | seconds(32bit)
    // 2^32 = 4294967296
    // 2^42 = 4398046511104
    private static final String CHANGE_ROOM_JOIN_NUM_SCRIPT = """
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not score then
                return 0
            end
            score = tonumber(score)
            
            -- 解析 score
            local seconds = score % 4294967296
            local rest = math.floor(score / 4294967296)
            local readyPlayers = rest % 1024
            local currentMaxPlayers = math.floor(rest / 1024)
            
            local maxLimit = tonumber(ARGV[2])
            local totalNum = tonumber(ARGV[3])
            local waitNum = tonumber(ARGV[4])
            
            -- 校验逻辑
            if totalNum > 0 and (currentMaxPlayers + totalNum) > maxLimit then
                return 0
            end
            if totalNum < 0 and (currentMaxPlayers + totalNum) < 0 then
                return 0
            end
            
            -- 计算新值
            local newMax = currentMaxPlayers + totalNum
            if newMax < 0 then newMax = 0 end
            local newReady = readyPlayers + waitNum
            if newReady < 0 then newReady = 0 end
            
            -- 重新计算 score 并保存
            local newScore = (newMax * 4398046511104) + (newReady * 4294967296) + seconds
            redis.call('ZADD', KEYS[1], newScore, ARGV[1])
            return 1
            """;
    private static final String GET_NEW_WAIT_JOIN_ROOM_SCRIPT = """
            -- 获取所有房间 (0, -1)
            local entries = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES')
            local maxLimit = tonumber(ARGV[1])
            local oldRoomId = ARGV[2] -- 传入可能是字符串
            
            -- ZRANGE 返回格式为 {member1, score1, member2, score2, ...}
            -- 步长为 2 遍历
            for i = 1, #entries, 2 do
                local member = entries[i]
                local score = tonumber(entries[i+1])
                -- 如果是旧房间，跳过
                if member == oldRoomId then
                    goto continue
                end
            
                -- 解析 score (参考 RoomScoreUtil)
                -- rest = maxPlayers(10bit) | readyPlayers(10bit)
                local rest = math.floor(score / 4294967296)
                -- currentMaxPlayers 是高 10 位
                local currentMaxPlayers = math.floor(rest / 1024)
            
                -- 判断是否满员
                if currentMaxPlayers < maxLimit then
                    return tonumber(member)
                end
                ::continue::
            end
            
            return 0
            """;

    private static final String CHECK_PLAYER_EXPIRED_SCRIPT = """
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not score then
                return 0
            end
            score = tonumber(score)
            
            local roomNum = tonumber(ARGV[2])
            local waitingNum = tonumber(ARGV[3])
            local diffCount = tonumber(ARGV[4])
            
            -- 解析 score
            local seconds = score % 4294967296
            local rest = math.floor(score / 4294967296)
            local readyPlayers = rest % 1024
            local maxPlayers = math.floor(rest / 1024)
            
            -- 逻辑校验
            local needFix = (maxPlayers ~= (roomNum + readyPlayers))
            local more = readyPlayers - waitingNum
            
            if more > 0 and (maxPlayers > more or needFix) then
                if diffCount > 2 then
                    -- 需要修正，计算新值
                    -- finalReadyPlayers = readyPlayers - more => readyPlayers - (readyPlayers - waitingNum) => waitingNum
                    local finalReadyPlayers = waitingNum
                    local newMaxPlayers = roomNum + finalReadyPlayers
                    -- 重新计算 score 并保存
                    local newScore = (newMaxPlayers * 4398046511104) + (finalReadyPlayers * 4294967296) + seconds
                    redis.call('ZADD', KEYS[1], newScore, ARGV[1])
                    return 0
                else
                    return diffCount + 1
                end
            end
            
            return diffCount
            """;

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
    public long getNewWaitJoinRoomId(int gameType, int roomConfigId, int maxLimit, long oldRoomId) {
        String matchRedisKey = getMatchRedisKey(gameType, roomConfigId);
        // 使用 Lua 脚本查找，移除分布式锁 @RedissonLock
        // 使用 LongCodec 确保返回值为 Long 类型
        return redissonClient.getScript(LongCodec.INSTANCE)
                .eval(RScript.Mode.READ_ONLY,
                        GET_NEW_WAIT_JOIN_ROOM_SCRIPT,
                        RScript.ReturnType.INTEGER,
                        Collections.singletonList(matchRedisKey),
                        maxLimit, oldRoomId);
    }

    /**
     * 将房间ID从房间中移除
     */
    public boolean removeWaitJoinRoomId(int gameType, int roomConfigId, long roomId) {
        String redisKey = getMatchRedisKey(gameType, roomConfigId);
        redissonClient.getScoredSortedSet(redisKey, LongCodec.INSTANCE).remove(roomId);
        return true;
    }

    /**
     * 添加房间等待ID
     */
    public boolean addWaitJoinRoomId(int gameType, int roomConfigId, long roomId, long roomCreateTime) {
        String redisKey = getMatchRedisKey(gameType, roomConfigId);
        RScoredSortedSet<Long> scoredSortedSet = redissonClient.getScoredSortedSet(redisKey, LongCodec.INSTANCE);
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
        String redisKey = getMatchRedisKey(gameType, roomConfigId);
        // 使用 Lua 脚本原子执行，无需再使用 redisLock
        // 使用 LongCodec 确保 roomId 作为 ARGV[1] 传递时与 ZSet 中的 member 格式兼容
        return redissonClient.getScript(LongCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE,
                        CHANGE_ROOM_JOIN_NUM_SCRIPT,
                        RScript.ReturnType.BOOLEAN,
                        Collections.singletonList(redisKey),
                        roomId, maxPlayerNum, totalNum, waitNum);
    }


    /**
     * 添加玩家进入房间时的过期等待
     *
     * @param roomId   房间id
     * @param playerId 玩家id
     */
    public void addPlayerExpiredWaiting(long roomId, long playerId) {
        String playerWaitKey = getPlayerWaitKey(roomId);
        RMapCache<Long, Long> waitMap = redissonClient.getMapCache(playerWaitKey);
        waitMap.put(playerId, System.currentTimeMillis(), 10, TimeUnit.SECONDS); // 自动刷新 TTL

    }

    /**
     * 添加获取玩家的等待人数
     *
     * @param roomId 房间id
     */
    public int getPlayerExpiredWaitingNum(long roomId) {
        String playerWaitKey = getPlayerWaitKey(roomId);
        return redissonClient.getMapCache(playerWaitKey).size();
    }

    /**
     * 检查玩家等待数据
     *
     * @param gameType     游戏类型
     * @param roomConfigId 房间配置id
     * @param room       房间信息
     */
    public int checkPlayerExpiredWaitingNum(int diffCount, int gameType, int roomConfigId, Room room) {
        String matchRedisKey = getMatchRedisKey(gameType, roomConfigId);
        long roomId = room.getId();

        // 获取过期等待人数 (RMapCache 的操作本身是原子的，直接在 Java 层获取)
        int waitingNum = getPlayerExpiredWaitingNum(roomId);
        int roomNum = room.getRoomPlayers().size();

        // 使用 Lua 脚本执行检查和更新，替代分布式锁
        // 返回值含义保持不变: 0=已修复或不存在, >diffCount=发现不一致但未修复, ==diffCount=正常
        Object eval = redissonClient.getScript(LongCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE,
                        CHECK_PLAYER_EXPIRED_SCRIPT,
                        RScript.ReturnType.INTEGER,
                        Collections.singletonList(matchRedisKey),
                        roomId, roomNum, waitingNum, diffCount);
        if (eval instanceof Long result) {
            if(result.intValue() != diffCount){
                log.error("房间数据不一致 diffCount = {} gameType = {} roomId = {}", diffCount, gameType, roomId);
            }
            return result.intValue();
        }
        return 0;
    }

}
