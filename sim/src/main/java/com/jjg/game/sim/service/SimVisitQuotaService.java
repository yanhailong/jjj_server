package com.jjg.game.sim.service;

import com.jjg.game.sim.data.SimVisitTrialSession;
import com.jjg.game.sim.constant.SimVisitConstant;
import org.redisson.api.RBucket;
import org.redisson.api.RBatch;
import org.redisson.api.RFuture;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;

/**
 * 拜访系统每日限额与短期会话。所有 key 均自然日过期，不在 JVM 内保存玩家计数。
 *
 * @author 11
 * @date 2026/6/30
 */
@Service
public class SimVisitQuotaService {
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long VISIT_KEY_TTL_SECONDS = Duration.ofDays(3).toSeconds();
    private static final int MAX_VISITED_TARGETS = 1000;
    private static final String CONSUME_LUA = """
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local amount = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            if amount <= 0 or current + amount > limit then
                return -1
            end
            local value = redis.call('INCRBY', KEYS[1], amount)
            redis.call('EXPIREAT', KEYS[1], ARGV[3])
            return limit - value
            """;
    private static final String ROLLBACK_LUA = """
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local amount = tonumber(ARGV[1])
            local value = current - amount
            if value <= 0 then
                redis.call('DEL', KEYS[1])
                return 0
            end
            redis.call('SET', KEYS[1], value)
            redis.call('EXPIREAT', KEYS[1], ARGV[2])
            return value
            """;
    private static final String CONSUME_UP_TO_LUA = """
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local amount = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local grant = math.min(amount, limit - current)
            if grant <= 0 then
                return 0
            end
            redis.call('INCRBY', KEYS[1], grant)
            redis.call('EXPIREAT', KEYS[1], ARGV[3])
            return grant
            """;
    private static final String MARK_VISITOR_LUA = """
            redis.call('PFADD', KEYS[1], ARGV[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return 1
            """;
    private static final String MARK_TARGET_VISITED_LUA = """
            if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 0
                    and redis.call('SCARD', KEYS[1]) < tonumber(ARGV[2]) then
                redis.call('SADD', KEYS[1], ARGV[1])
            end
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
            """;

    private final RedissonClient redissonClient;
    private final RScript script;

    public SimVisitQuotaService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.script = redissonClient.getScript(StringCodec.INSTANCE);
    }

    public int consume(String type, long playerId, int amount, int limit) {
        long remaining = consumeLong(type, playerId, amount, limit);
        return remaining < 0 ? -1 : (int) remaining;
    }

    public long consumeLong(String type, long playerId, long amount, long limit) {
        String key = dailyKey(type, playerId, LocalDate.now());
        Long remaining = script.eval(RScript.Mode.READ_WRITE, CONSUME_LUA,
                RScript.ReturnType.INTEGER, Collections.singletonList(key),
                amount, limit, expireEpochSecond());
        return remaining == null ? -1 : remaining;
    }

    /**
     * 在额度内尽量扣减并返回实际扣减值，适用于允许吃满剩余额度的金币抽成。
     */
    public long consumeUpToLong(String type, long playerId, long amount, long limit) {
        if (amount <= 0 || limit <= 0) {
            return 0;
        }
        String key = dailyKey(type, playerId, LocalDate.now());
        Long consumed = script.eval(RScript.Mode.READ_WRITE, CONSUME_UP_TO_LUA,
                RScript.ReturnType.INTEGER, Collections.singletonList(key),
                amount, limit, expireEpochSecond());
        return consumed == null ? 0 : consumed;
    }

    public void rollback(String type, long playerId, int amount) {
        rollbackLong(type, playerId, amount);
    }

    public void rollbackLong(String type, long playerId, long amount) {
        if (amount <= 0) {
            return;
        }
        script.eval(RScript.Mode.READ_WRITE, ROLLBACK_LUA,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(dailyKey(type, playerId, LocalDate.now())),
                amount, expireEpochSecond());
    }

    public int remaining(String type, long playerId, int limit) {
        return (int) remainingLong(type, playerId, limit);
    }

    public long remainingLong(String type, long playerId, long limit) {
        String value = redissonClient.<String>getBucket(dailyKey(type, playerId, LocalDate.now()), StringCodec.INSTANCE).get();
        long used = value == null ? 0 : Long.parseLong(value);
        return Math.max(0, limit - used);
    }

    public long used(String type, long playerId) {
        String value = redissonClient.<String>getBucket(dailyKey(type, playerId, LocalDate.now()), StringCodec.INSTANCE).get();
        return value == null ? 0 : Long.parseLong(value);
    }

    /**
     * 拜访快照一次批量读取三类访客次数与房主今日人气，减少 Redis 往返。
     */
    public VisitQuotaSnapshot visitSnapshot(long visitorId, long ownerId,
                                             int likeLimit, int commentLimit, int trialLimit) {
        LocalDate day = LocalDate.now();
        RBatch batch = redissonClient.createBatch();
        RFuture<String> like = batch.<String>getBucket(
                dailyKey(SimVisitConstant.QuotaType.LIKE, visitorId, day),
                StringCodec.INSTANCE).getAsync();
        RFuture<String> comment = batch.<String>getBucket(
                dailyKey(SimVisitConstant.QuotaType.COMMENT, visitorId, day),
                StringCodec.INSTANCE).getAsync();
        RFuture<String> trial = batch.<String>getBucket(
                dailyKey(SimVisitConstant.QuotaType.TRIAL, visitorId, day),
                StringCodec.INSTANCE).getAsync();
        RFuture<String> popularity = batch.<String>getBucket(
                dailyKey(SimVisitConstant.QuotaType.POPULARITY, ownerId, day),
                StringCodec.INSTANCE).getAsync();
        batch.execute();
        return new VisitQuotaSnapshot(
                remainingValue(like.toCompletableFuture().join(), likeLimit),
                remainingValue(comment.toCompletableFuture().join(), commentLimit),
                remainingValue(trial.toCompletableFuture().join(), trialLimit),
                usedValue(popularity.toCompletableFuture().join()));
    }

    public void markVisited(long ownerId, long visitorId) {
        script.eval(RScript.Mode.READ_WRITE, MARK_VISITOR_LUA, RScript.ReturnType.INTEGER,
                Collections.singletonList(dailyKey("visitors", ownerId, LocalDate.now())),
                String.valueOf(visitorId), VISIT_KEY_TTL_SECONDS);
    }

    public void markTargetVisited(long visitorId, long ownerId) {
        script.eval(RScript.Mode.READ_WRITE, MARK_TARGET_VISITED_LUA, RScript.ReturnType.INTEGER,
                Collections.singletonList(dailyKey("visited-targets", visitorId, LocalDate.now())),
                String.valueOf(ownerId), MAX_VISITED_TARGETS, VISIT_KEY_TTL_SECONDS);
    }

    public Set<Long> visitedTargets(long visitorId) {
        Set<String> values = redissonClient.<String>getSet(
                dailyKey("visited-targets", visitorId, LocalDate.now()), StringCodec.INSTANCE).readAll();
        return values.stream().map(Long::parseLong).collect(java.util.stream.Collectors.toSet());
    }

    public long visitorCount(long ownerId) {
        return redissonClient.<String>getHyperLogLog(
                dailyKey("visitors", ownerId, LocalDate.now()), StringCodec.INSTANCE).count();
    }

    public void saveTrialSession(SimVisitTrialSession session, int seconds) {
        redissonClient.<SimVisitTrialSession>getBucket(trialSessionKey(session.getVisitorId()))
                .set(session, Duration.ofSeconds(seconds));
    }

    public SimVisitTrialSession getTrialSession(long visitorId) {
        return redissonClient.<SimVisitTrialSession>getBucket(trialSessionKey(visitorId)).get();
    }

    public boolean deleteTrialSession(long visitorId) {
        RBucket<SimVisitTrialSession> bucket = redissonClient.getBucket(trialSessionKey(visitorId));
        return bucket.delete();
    }

    public void savePendingPermit(long visitorId, String permitId, int seconds) {
        redissonClient.<String>getBucket(pendingPermitKey(visitorId, permitId), StringCodec.INSTANCE)
                .set("1", Duration.ofSeconds(seconds));
    }

    public boolean consumePendingPermit(long visitorId, String permitId) {
        if (permitId == null || permitId.isBlank()) {
            return false;
        }
        return redissonClient.<String>getBucket(
                pendingPermitKey(visitorId, permitId), StringCodec.INSTANCE).delete();
    }

    public static String dailyKey(String type, long playerId, LocalDate day) {
        return "sim:visit:quota:" + DAY_FORMAT.format(day) + ":" + type + ":" + playerId;
    }

    private String trialSessionKey(long visitorId) {
        return "sim:visit:trial:" + visitorId;
    }

    private String pendingPermitKey(long visitorId, String permitId) {
        return "sim:visit:trial-pending:" + visitorId + ":" + permitId;
    }

    private long expireEpochSecond() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return now.toLocalDate().plusDays(2).atStartOfDay(now.getZone()).toEpochSecond();
    }

    private int remainingValue(String value, int limit) {
        return Math.max(0, limit - usedValue(value));
    }

    private int usedValue(String value) {
        if (value == null) {
            return 0;
        }
        long used = Long.parseLong(value);
        return used >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, used);
    }

    public record VisitQuotaSnapshot(int remainingLikes, int remainingComments,
                                     int remainingTrials, int ownerTodayPopularity) {
    }
}
