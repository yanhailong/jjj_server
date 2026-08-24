package com.jjg.game.slots.dao;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class TogetherPlayDao {
    private static final String KEY_PREFIX = "togetherPlay:";
    private static final String LEAVE_SCRIPT = """
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            redis.call('ZREM', KEYS[1], ARGV[1])
            return score
            """;

    private final RedissonClient redissonClient;

    public TogetherPlayDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void enter(int gameType, long playerId, long winGold) {
        zset(gameType).add(winGold, playerId);
    }

    public Long leave(int gameType, long playerId) {
        Object score = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, LEAVE_SCRIPT, RScript.ReturnType.VALUE,
                Collections.singletonList(key(gameType)), String.valueOf(playerId));
        return score == null ? null : Double.valueOf(score.toString()).longValue();
    }

    public void remove(int gameType, long playerId) {
        zset(gameType).remove(playerId);
    }

    public void addWinGold(int gameType, long playerId, long change) {
        if (change != 0) {
            zset(gameType).addScore(playerId, change);
        }
    }

    public List<PlayerScore> page(int gameType, int start, int count) {
        if (count <= 0) {
            return List.of();
        }
        Collection<ScoredEntry<Long>> entries =
                zset(gameType).entryRangeReversed(start, start + count - 1);
        List<PlayerScore> result = new ArrayList<>(entries.size());
        for (ScoredEntry<Long> entry : entries) {
            result.add(new PlayerScore(entry.getValue(), entry.getScore().longValue()));
        }
        return result;
    }

    public List<Double> scores(int gameType, List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        return zset(gameType).getScore(playerIds);
    }

    private RScoredSortedSet<Long> zset(int gameType) {
        return redissonClient.getScoredSortedSet(key(gameType), LongCodec.INSTANCE);
    }

    private String key(int gameType) {
        return KEY_PREFIX + gameType;
    }

    public record PlayerScore(long playerId, long winGold) {
    }
}
