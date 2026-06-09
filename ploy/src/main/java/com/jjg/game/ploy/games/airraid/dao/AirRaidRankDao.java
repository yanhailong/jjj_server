package com.jjg.game.ploy.games.airraid.dao;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.ploy.games.airraid.pb.AirRaidPlayerInfo;
import com.jjg.game.ploy.games.airraid.pb.AirRaidRankInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2026/5/13
 */
@Repository
public class AirRaidRankDao {
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 坠毁倍数榜 key
    private final String KEY_MUL_DAY = "airraid:rank:mul:d:%s";
    private final String KEY_MUL_MONTH = "airraid:rank:mul:m:%s";
    private final String KEY_MUL_YEAR = "airraid:rank:mul:y:%s";
    // 中奖金额榜 key
    private final String KEY_WIN_DAY = "airraid:rank:win:d:%s";
    private final String KEY_WIN_MONTH = "airraid:rank:win:m:%s";
    private final String KEY_WIN_YEAR = "airraid:rank:win:y:%s";

    // 坠毁倍数榜（只有倍数）
    private final String KEY_ROUND_DAY = "airraid:rank:round:d:%s";
    private final String KEY_ROUND_MONTH = "airraid:rank:round:m:%s";
    private final String KEY_ROUND_YEAR = "airraid:rank:round:y:%s";

    private static final int MAX_RANK_SIZE = 20;

    // 旧 Lua：坠毁时记录回合最高倍数
    private final DefaultRedisScript<Long> addRankScript = new DefaultRedisScript<>();
    private final String script = """
            local times   = tonumber(ARGV[1])
            local member  = ARGV[2]
            local maxSize = tonumber(ARGV[3])
            local expiries = {tonumber(ARGV[4]), tonumber(ARGV[5]), tonumber(ARGV[6])}
            for i = 1, 3 do
                redis.call('ZADD', KEYS[i], times, member)
                local size = redis.call('ZCARD', KEYS[i])
                if size > maxSize then
                    redis.call('ZREMRANGEBYRANK', KEYS[i], 0, size - maxSize - 1)
                end
                redis.call('EXPIREAT', KEYS[i], expiries[i])
            end
            return 1
            """;

    // 兑现排行榜 Lua：一次原子写 6 个 ZSet（倍数榜×3 + 金额榜×3）
    private final DefaultRedisScript<Long> addCashOutRankScript = new DefaultRedisScript<>();
    private final String cashOutScript = """
            local mulScore = tonumber(ARGV[1])
            local winScore = tonumber(ARGV[2])
            local member   = ARGV[3]
            local maxSize  = tonumber(ARGV[4])
            local expiries = {tonumber(ARGV[5]), tonumber(ARGV[6]), tonumber(ARGV[7])}
            for i = 1, 3 do
                redis.call('ZADD', KEYS[i], mulScore, member)
                local size = redis.call('ZCARD', KEYS[i])
                if size > maxSize then
                    redis.call('ZREMRANGEBYRANK', KEYS[i], 0, size - maxSize - 1)
                end
                redis.call('EXPIREAT', KEYS[i], expiries[i])
            end
            for i = 4, 6 do
                redis.call('ZADD', KEYS[i], winScore, member)
                local size = redis.call('ZCARD', KEYS[i])
                if size > maxSize then
                    redis.call('ZREMRANGEBYRANK', KEYS[i], 0, size - maxSize - 1)
                end
                redis.call('EXPIREAT', KEYS[i], expiries[i - 3])
            end
            return 1
            """;

    /**
     * 兑现成功时记录到 6 个排行榜（倍数榜日/月/年 + 金额榜日/月/年）。
     */
    public void addCashOut(long playerId, int headImgId, int headFrame, long time, long bet,
                           long winAmount, int cashOutMultiplier, int crashMultiplier, int betIndex,
                           int roundId, String nick) {
        AirRaidRankInfo rankInfo = new AirRaidRankInfo();
        rankInfo.playerInfo = new AirRaidPlayerInfo();
        rankInfo.playerInfo.playerId = playerId;
        rankInfo.playerInfo.headImgId = headImgId;
        rankInfo.playerInfo.headFrame = headFrame;
        rankInfo.playerInfo.cashOutMultiplier = cashOutMultiplier;
        rankInfo.playerInfo.bet = bet;
        rankInfo.playerInfo.winAmount = winAmount;
        rankInfo.playerInfo.betIndex = betIndex;
        rankInfo.playerInfo.nick = nick;

        rankInfo.crashMultiplier = crashMultiplier;
        rankInfo.roundId = roundId;
        rankInfo.time = time;
        String member = JSON.toJSONString(rankInfo);

        String mulDayKey = KEY_MUL_DAY.formatted(TimeHelper.getDate(time, "yyMMdd"));
        String mulMonthKey = KEY_MUL_MONTH.formatted(TimeHelper.getDate(time, "yyMM"));
        String mulYearKey = KEY_MUL_YEAR.formatted(TimeHelper.getDate(time, "yy"));
        String winDayKey = KEY_WIN_DAY.formatted(TimeHelper.getDate(time, "yyMMdd"));
        String winMonthKey = KEY_WIN_MONTH.formatted(TimeHelper.getDate(time, "yyMM"));
        String winYearKey = KEY_WIN_YEAR.formatted(TimeHelper.getDate(time, "yy"));

        long dayExpireAt = TimeHelper.getEndOfDayTimestamp(time) / 1000;
        long monthExpireAt = endOfMonth(time) / 1000;
        long yearExpireAt = endOfYear(time) / 1000;

        addCashOutRankScript.setResultType(Long.class);
        addCashOutRankScript.setScriptText(cashOutScript);
        redisTemplate.execute(
                addCashOutRankScript,
                List.of(mulDayKey, mulMonthKey, mulYearKey, winDayKey, winMonthKey, winYearKey),
                String.valueOf(cashOutMultiplier),
                String.valueOf(winAmount),
                member,
                String.valueOf(MAX_RANK_SIZE),
                String.valueOf(dayExpireAt),
                String.valueOf(monthExpireAt),
                String.valueOf(yearExpireAt)
        );
    }

    /**
     * 中奖倍数排行榜。period: 1=日 2=月 3=年。
     * 仅当某条记录所属回合不是"当前未坠毁的回合"时才下发其 crashMultiplier，避免泄露未结束局的坠毁倍数。
     */
    public List<AirRaidRankInfo> getMultiplierRank(int period, int currentRoundId, boolean currentRoundCrashed) {
        return parseRank(mulKey(period), currentRoundId, currentRoundCrashed);
    }

    /**
     * 中奖金额排行榜。period: 1=日 2=月 3=年。
     * 仅当某条记录所属回合不是"当前未坠毁的回合"时才下发其 crashMultiplier，避免泄露未结束局的坠毁倍数。
     */
    public List<AirRaidRankInfo> getWinRank(int period, int currentRoundId, boolean currentRoundCrashed) {
        return parseRank(winKey(period), currentRoundId, currentRoundCrashed);
    }

    /**
     * 回合赔率排行榜。period: 1=日 2=月 3=年
     */
    public List<AirRaidRankInfo> getRoundRank(int period) {
        return parseRoundRank(roundKey(period));
    }

    private String mulKey(int period) {
        long now = System.currentTimeMillis();
        return switch (period) {
            case 2 -> KEY_MUL_MONTH.formatted(TimeHelper.getDate(now, "yyMM"));
            case 3 -> KEY_MUL_YEAR.formatted(TimeHelper.getDate(now, "yy"));
            default -> KEY_MUL_DAY.formatted(TimeHelper.getDate(now, "yyMMdd"));
        };
    }

    private String winKey(int period) {
        long now = System.currentTimeMillis();
        return switch (period) {
            case 2 -> KEY_WIN_MONTH.formatted(TimeHelper.getDate(now, "yyMM"));
            case 3 -> KEY_WIN_YEAR.formatted(TimeHelper.getDate(now, "yy"));
            default -> KEY_WIN_DAY.formatted(TimeHelper.getDate(now, "yyMMdd"));
        };
    }

    private String roundKey(int period) {
        long now = System.currentTimeMillis();
        return switch (period) {
            case 2 -> KEY_ROUND_MONTH.formatted(TimeHelper.getDate(now, "yyMM"));
            case 3 -> KEY_ROUND_YEAR.formatted(TimeHelper.getDate(now, "yy"));
            default -> KEY_ROUND_DAY.formatted(TimeHelper.getDate(now, "yyMMdd"));
        };
    }

    private List<AirRaidRankInfo> parseRank(String key, int currentRoundId, boolean currentRoundCrashed) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, MAX_RANK_SIZE - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<AirRaidRankInfo> result = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            AirRaidRankInfo info = decodeMember(tuple.getValue());
            if (info == null) {
                continue;
            }
            // 当前回合还在飞行/下注阶段时，屏蔽该回合 entry 的坠毁倍数，避免泄露
            if (!currentRoundCrashed && info.roundId == currentRoundId && currentRoundId > 0) {
                continue;
            }
            result.add(info);
        }
        return result;
    }

    private List<AirRaidRankInfo> parseRoundRank(String key) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, MAX_RANK_SIZE - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<AirRaidRankInfo> result = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            AirRaidRankInfo info = new AirRaidRankInfo();
            info.time = Long.parseLong(tuple.getValue());
            info.crashMultiplier = tuple.getScore().intValue();
            result.add(info);
        }
        return result;
    }

    private AirRaidRankInfo decodeMember(String member) {
        if (member == null) {
            return null;
        }
        try {
            return JSON.parseObject(member, AirRaidRankInfo.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 坠毁时记录回合最高倍数
     *
     * @param timestamp
     * @param times
     */
    public void add(long timestamp, long times) {
        String dayKey = KEY_ROUND_DAY.formatted(TimeHelper.getDate(timestamp, "yyMMdd"));
        String monthKey = KEY_ROUND_MONTH.formatted(TimeHelper.getDate(timestamp, "yyMM"));
        String yearKey = KEY_ROUND_YEAR.formatted(TimeHelper.getDate(timestamp, "yy"));

        long dayExpireAt = TimeHelper.getEndOfDayTimestamp(timestamp) / 1000;
        long monthExpireAt = endOfMonth(timestamp) / 1000;
        long yearExpireAt = endOfYear(timestamp) / 1000;

        addRankScript.setResultType(Long.class);
        addRankScript.setScriptText(script);
        redisTemplate.execute(
                addRankScript,
                List.of(dayKey, monthKey, yearKey),
                String.valueOf(times),
                String.valueOf(timestamp),
                String.valueOf(MAX_RANK_SIZE),
                String.valueOf(dayExpireAt),
                String.valueOf(monthExpireAt),
                String.valueOf(yearExpireAt)
        );
    }

    private long endOfMonth(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .with(TemporalAdjusters.lastDayOfMonth())
                .with(LocalTime.MAX)
                .truncatedTo(ChronoUnit.MILLIS)
                .toInstant()
                .toEpochMilli();
    }

    private long endOfYear(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .with(TemporalAdjusters.lastDayOfYear())
                .with(LocalTime.MAX)
                .truncatedTo(ChronoUnit.MILLIS)
                .toInstant()
                .toEpochMilli();
    }
}
