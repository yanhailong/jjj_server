package com.jjg.game.core.service;


import com.jjg.game.core.data.RankChange;
import com.jjg.game.core.data.RankEntry;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author lm
 * @date 2025/12/24 15:06
 */
@Service
public class RankService {

    private static final long TIME_BITS = 23;
    private static final long TIME_MASK = (1L << TIME_BITS) - 1; // 8388607
    private static final long POINTS_MAX = (1L << 30) - 1;      // 1073741823
    private final RedissonClient redissonClient;
    private final RScript script;


    /**
     * -- KEYS[1] rank key
     * -- ARGV[1] member
     * -- ARGV[2] addPoints
     * -- ARGV[3] nowTime
     */
    private static final String SINGLE_ADD_LUA = """
            
            local BASE = 8388608
            local TIME_MAX  = 8388607
            local POINTS_MAX = 1073741823
            
            local key = KEYS[1]
            local member = ARGV[1]
            local addPoints = tonumber(ARGV[2])
            local nowTime = tonumber(ARGV[3])
            
            local oldScore = redis.call('ZSCORE', key, member)
            
            local points
            if not oldScore then
                points = addPoints
            else
                points = math.floor(tonumber(oldScore) / BASE) + addPoints
            end
            
            if points < 0 then points = 0 end
            if points > POINTS_MAX then points = POINTS_MAX end
            
            local newScore = points * BASE + (TIME_MAX - nowTime)
            redis.call('ZADD', key, newScore, member)
            
            return newScore
            """;
    /**
     * -- KEYS[1] rank key
     * -- ARGV = member1, add1, member2, add2, ..., nowTime
     */
    private static final String BATCH_SET_LUA = """
            local BASE = 8388608
            local TIME_MAX  = 8388607
            local POINTS_MAX = 1073741823
            
            local key = KEYS[1]
            local argc = #ARGV
            local nowTime = tonumber(ARGV[argc])
            for i = 1, argc - 1, 2 do
                local member = ARGV[i]
                local points = tonumber(ARGV[i + 1])
            
                if points < 0 then points = 0 end
                if points > POINTS_MAX then points = POINTS_MAX end
            
                local newScore = points * BASE + (TIME_MAX - nowTime)
                redis.call('ZADD', key, newScore, member)
            end
            """;
    private static final String BATCH_ADD_LUA = """
            local BASE = 8388608
            local TIME_MAX  = 8388607
            local POINTS_MAX = 1073741823
            
            local key = KEYS[1]
            local argc = #ARGV
            local nowTime = tonumber(ARGV[argc])
            for i = 1, argc - 1, 2 do
                local member = ARGV[i]
                local addPoints = tonumber(ARGV[i + 1])
            
                local oldScore = redis.call('ZSCORE', key, member)
                local points
            
                if not oldScore then
                    points = addPoints
                else
                    points = math.floor(tonumber(oldScore) / BASE) + addPoints
                end
            
                if points < 0 then points = 0 end
                if points > POINTS_MAX then points = POINTS_MAX end
            
                local newScore = points * BASE + (TIME_MAX - nowTime)
                redis.call('ZADD', key, newScore, member)
            end
            """;

    public RankService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.script = redissonClient.getScript(LongCodec.INSTANCE);
    }

    /**
     * 增加玩家分数
     *
     * @param rankKey   排行榜 key
     * @param playerId  玩家 ID
     * @param addPoints 增加的分数
     * @return 增加后的分数
     */
    public long addPoints(String rankKey, long playerId, int addPoints) {
        long now = (System.currentTimeMillis() / 1000) & TIME_MASK;
        Long score = script.eval(
                RScript.Mode.READ_WRITE,
                SINGLE_ADD_LUA,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(rankKey),
                playerId,
                addPoints,
                now
        );
        return score >>> TIME_BITS;
    }

    /**
     * 批量设置玩家分数
     *
     * @param rankKey 排行榜 key
     * @param changes 玩家分数变化列表
     */
    public void batchSetPoints(String rankKey, List<RankChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        List<Object> args = new ArrayList<>();
        for (RankChange c : changes) {
            args.add(c.getPlayerId());
            args.add(c.getAddPoints());
        }
        args.add((System.currentTimeMillis() / 1000) & TIME_MASK);
        script.eval(
                RScript.Mode.READ_WRITE,
                BATCH_SET_LUA,
                RScript.ReturnType.STATUS,
                Collections.singletonList(rankKey),
                args.toArray()
        );
    }

    /**
     * 获取按从大到小前N名
     *
     * @param rankKey 排行榜 key
     * @param n       前N名
     * @return 前N名的排行数据
     */
    public List<RankEntry> topN(String rankKey, int n) {
        if (n <= 0) {
            return List.of();
        }
        RScoredSortedSet<Long> zset =
                redissonClient.getScoredSortedSet(rankKey, LongCodec.INSTANCE);
        Collection<ScoredEntry<Long>> entries =
                zset.entryRangeReversed(0, n - 1);

        List<RankEntry> result = new ArrayList<>();
        long rank = 1;

        for (ScoredEntry<Long> e : entries) {
            long score = e.getScore().longValue();
            Number value = e.getValue();
            long points = score >>> TIME_BITS;
            result.add(new RankEntry(
                    value.longValue(),
                    points,
                    rank++
            ));
        }
        return result;
    }

    /**
     * 批量获取玩家积分
     *
     * @param rankKey   排行榜 key
     * @param playerIds 玩家 ID 列表
     * @return Map<玩家ID, 积分> (如果玩家不存在则积分为 0)
     */
    public Map<Long, Long> batchGetPoints(String rankKey, List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        RScoredSortedSet<Long> zset =
                redissonClient.getScoredSortedSet(rankKey, LongCodec.INSTANCE);

        // 使用 Redisson 批量获取分数
        List<Double> scores = zset.getScore(playerIds);
        Map<Long, Long> resultMap = new HashMap<>(playerIds.size());

        for (int i = 0; i < playerIds.size(); i++) {
            Long playerId = playerIds.get(i);
            Double score = scores.get(i);

            long points = (score == null) ? 0L : (score.longValue() >>> TIME_BITS);
            resultMap.put(playerId, points);
        }
        return resultMap;
    }

    /**
     * 批量添加积分
     *
     * @param rankKey 排行榜 key
     * @param changes 变化的积分
     */
    public void batchAddPoints(String rankKey, List<RankChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        List<Object> args = new ArrayList<>(changes.size());
        for (RankChange c : changes) {
            args.add(c.getPlayerId());
            args.add(c.getAddPoints());
        }
        args.add((System.currentTimeMillis() / 1000) & TIME_MASK);
        script.eval(
                RScript.Mode.READ_WRITE,
                BATCH_ADD_LUA,
                RScript.ReturnType.STATUS,
                Collections.singletonList(rankKey),
                args.toArray()
        );
    }

    /**
     * 获取排名
     *
     * @param rankKey  排行榜 key
     * @param playerId 玩家 ID
     * @return 排名信息，如果玩家不存在则返回 null
     */
    public RankEntry getRank(String rankKey, long playerId) {
        RScoredSortedSet<Long> zset =
                redissonClient.getScoredSortedSet(rankKey, LongCodec.INSTANCE);

        Integer rank = zset.revRank(playerId);
        Double score = zset.getScore(playerId);

        if (rank == null || score == null) {
            return null;
        }

        long points = score.longValue() >>> TIME_BITS;
        return new RankEntry(playerId, points, rank + 1);
    }

    /**
     * 获取指定玩家分数
     *
     * @param rankKey  排行榜的 key
     * @param playerId 玩家 ID
     * @return 当前分数
     */
    public long getPoints(String rankKey, long playerId) {
        RScoredSortedSet<Long> zset =
                redissonClient.getScoredSortedSet(rankKey, LongCodec.INSTANCE);
        Double score = zset.getScore(playerId);
        if (score == null) {
            return 0;
        }
        return score.longValue() >>> TIME_BITS;
    }

    /**
     * 清除排行榜
     *
     * @param rankKey 排行榜 key
     */
    public void reset(String rankKey) {
        redissonClient.getScoredSortedSet(rankKey).delete();
    }
}
