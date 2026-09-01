package com.jjg.game.slots.dao;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class TogetherPlayDao {
    private static final String KEY_PREFIX = "togetherPlay:";
    private static final String EFFECT_KEY_PREFIX = "togetherPlay:effect:";
    private static final String INVITEE_KEY_PREFIX = "togetherPlay:invitee:";
    private static final String INVITER_KEY_PREFIX = "togetherPlay:inviter:";
    private static final String COMMISSION_KEY_PREFIX = "togetherPlay:commission:";
    private static final String LEAVE_SCRIPT = """
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            redis.call('ZREM', KEYS[1], ARGV[1])
            redis.call('HDEL', KEYS[2], ARGV[1])
            return score
            """;
    private static final String RESET_SESSION_SCRIPT = """
            local playerId = ARGV[4]
            local invitees = redis.call('ZRANGE', KEYS[1], 0, -1)
            for _, inviteeId in ipairs(invitees) do
                redis.call('ZREM', ARGV[1] .. inviteeId, playerId)
                redis.call('HDEL', ARGV[3] .. inviteeId, playerId)
            end
            local inviters = redis.call('ZRANGE', KEYS[2], 0, -1)
            for _, inviterId in ipairs(inviters) do
                redis.call('ZREM', ARGV[2] .. inviterId, playerId)
                redis.call('HDEL', ARGV[3] .. inviterId, playerId)
            end
            redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
            return #invitees + #inviters
            """;
    private static final String ADD_COMMISSION_SCRIPT = """
            return redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2])
            """;
    private static final String RECORD_INVITE_SCRIPT = """
            local latest = redis.call('ZREVRANGE', KEYS[1], 0, 0, 'WITHSCORES')
            local order = #latest == 0 and 1 or tonumber(latest[2]) + 1
            redis.call('ZADD', KEYS[1], order, ARGV[1])
            redis.call('ZADD', KEYS[2], ARGV[2], ARGV[3])
            return 1
            """;

    private final RedissonClient redissonClient;

    public TogetherPlayDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void enter(int gameType, long playerId, long winGold,
                      int commissionUsers, int winCommission) {
        zset(gameType).add(winGold, playerId);
        effectMap(gameType).put(String.valueOf(playerId),
                commissionUsers + ":" + winCommission);
    }

    public Long leave(int gameType, long playerId) {
        return removePlayer(gameType, playerId);
    }

    public void remove(int gameType, long playerId) {
        removePlayer(gameType, playerId);
    }

    private Long removePlayer(int gameType, long playerId) {
        Object score = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, LEAVE_SCRIPT, RScript.ReturnType.VALUE,
                List.of(key(gameType), effectKey(gameType)), String.valueOf(playerId));
        return score == null ? null : Double.valueOf(score.toString()).longValue();
    }

    public void resetSession(int gameType, long playerId) {
        redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, RESET_SESSION_SCRIPT, RScript.ReturnType.INTEGER,
                List.of(inviteeKey(gameType, playerId), inviterKey(gameType, playerId),
                        commissionKey(gameType, playerId)),
                inviterKeyPrefix(gameType), inviteeKeyPrefix(gameType), commissionKeyPrefix(gameType),
                String.valueOf(playerId));
    }

    public void recordInvite(int gameType, long inviterId, long inviteeId) {
        redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, RECORD_INVITE_SCRIPT, RScript.ReturnType.INTEGER,
                List.of(inviteeKey(gameType, inviterId), inviterKey(gameType, inviteeId)),
                String.valueOf(inviteeId), String.valueOf(System.currentTimeMillis()),
                String.valueOf(inviterId));
    }

    public List<Long> invitees(int gameType, long inviterId, int start, int count) {
        return range(invitees(gameType, inviterId), start, count);
    }

    public List<Long> inviters(int gameType, long inviteeId, int start, int count) {
        return range(inviters(gameType, inviteeId), start, count);
    }

    public boolean hasInvite(int gameType, long inviterId, long inviteeId) {
        return invitees(gameType, inviterId).contains(inviteeId);
    }

    public void removeInviter(int gameType, long inviteeId, long inviterId) {
        inviters(gameType, inviteeId).remove(inviterId);
    }

    public Map<Long, CommissionEffect> effects(int gameType, Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Map.of();
        }
        Set<String> keys = new HashSet<>();
        for (Long playerId : playerIds) {
            keys.add(String.valueOf(playerId));
        }
        Map<String, String> values = effectMap(gameType).getAll(keys);
        Map<Long, CommissionEffect> result = new HashMap<>();
        for (Map.Entry<String, String> en : values.entrySet()) {
            String[] parts = en.getValue().split(":", 2);
            if (parts.length == 2) {
                result.put(Long.parseLong(en.getKey()), new CommissionEffect(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
            }
        }
        return result;
    }

    public void addCommission(int gameType, long playerId, long sourcePlayerId, long amount) {
        redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, ADD_COMMISSION_SCRIPT, RScript.ReturnType.INTEGER,
                Collections.singletonList(commissionKey(gameType, playerId)),
                String.valueOf(sourcePlayerId), String.valueOf(amount));
    }

    public Map<Long, Long> commissions(int gameType, long playerId, Collection<Long> sourcePlayerIds) {
        if (sourcePlayerIds == null || sourcePlayerIds.isEmpty()) {
            return Map.of();
        }
        Set<String> keys = new HashSet<>();
        for (Long sourcePlayerId : sourcePlayerIds) {
            keys.add(String.valueOf(sourcePlayerId));
        }
        Map<String, String> values = commissionMap(gameType, playerId).getAll(keys);
        Map<Long, Long> result = new HashMap<>();
        values.forEach((key, value) -> result.put(Long.parseLong(key), Long.parseLong(value)));
        return result;
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

    public int count(int gameType) {
        return zset(gameType).size();
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

    private RScoredSortedSet<Long> invitees(int gameType, long inviterId) {
        return redissonClient.getScoredSortedSet(
                inviteeKey(gameType, inviterId), LongCodec.INSTANCE);
    }

    private RScoredSortedSet<Long> inviters(int gameType, long inviteeId) {
        return redissonClient.getScoredSortedSet(
                inviterKey(gameType, inviteeId), LongCodec.INSTANCE);
    }

    private RMap<String, String> effectMap(int gameType) {
        return redissonClient.getMap(effectKey(gameType), StringCodec.INSTANCE);
    }

    private RMap<String, String> commissionMap(int gameType, long playerId) {
        return redissonClient.getMap(commissionKey(gameType, playerId), StringCodec.INSTANCE);
    }

    private List<Long> range(RScoredSortedSet<Long> set, int start, int count) {
        if (start < 0 || count <= 0) {
            return List.of();
        }
        return new ArrayList<>(set.valueRange(start, start + count - 1));
    }

    static String key(int gameType) {
        return KEY_PREFIX + gameType;
    }

    private String effectKey(int gameType) {
        return EFFECT_KEY_PREFIX + gameType;
    }

    private String commissionKey(int gameType, long playerId) {
        return commissionKeyPrefix(gameType) + playerId;
    }

    private String inviteeKey(int gameType, long inviterId) {
        return inviteeKeyPrefix(gameType) + inviterId;
    }

    private String inviterKey(int gameType, long inviteeId) {
        return inviterKeyPrefix(gameType) + inviteeId;
    }

    private String commissionKeyPrefix(int gameType) {
        return COMMISSION_KEY_PREFIX + gameType + ":";
    }

    private String inviteeKeyPrefix(int gameType) {
        return INVITEE_KEY_PREFIX + gameType + ":";
    }

    private String inviterKeyPrefix(int gameType) {
        return INVITER_KEY_PREFIX + gameType + ":";
    }

    public record PlayerScore(long playerId, long winGold) {
    }

    public record CommissionEffect(int commissionUsers, int winCommission) {
    }
}
