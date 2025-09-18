package com.jjg.game.activity.sharepromote.dao;

import com.jjg.game.activity.sharepromote.data.SharePromotePlayerData;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * SharePromoteDao
 * <p>
 * 推广分享活动数据访问层
 * 负责玩家推广绑定、收益统计、排行榜管理、唯一邀请码生成等 Redis 操作
 * 设计要点：
 * 1. 使用 Redis Hash、Set、ZSet、String 类型存储不同类型数据
 * 2. 提供排行榜分页查询和个人收益查询
 * 3. 支持全局唯一邀请码生成
 * 4. 支持每日收入自动过期
 * </p>
 *
 * @author lm
 * @date 2025/9/15
 */
@Repository
public class SharePromoteDao {

    private final Logger log = LoggerFactory.getLogger(SharePromoteDao.class);

    // 随机码生成配置
    private final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom RANDOM = new SecureRandom();
    private final int CODE_LENGTH = 12;

    // Redis 模板
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, SharePromotePlayerData> playerDataRedisTemplate;

    // Redis Key 模板
    private final String SHARE_PROMOTE_BIND_KEY = "activity:sharepromote:bind:%s";                  // source -> targets
    private final String SHARE_PROMOTE_ALREADY_BIND_KEY = "activity:sharepromote:alreadybind:%s";  // 被绑定玩家
    private final String SHARE_PROMOTE_CODE = "activity:sharepromote:code";                         // 全局唯一邀请码 Hash
    private final String SHARE_PROMOTE_REWARDS_RANK = "activity:sharepromote:rank";               // 总收益排行榜 ZSet
    private final String SHARE_PROMOTE_REWARDS_INCOME_RANK = "activity:sharepromote:incomerank:%d"; // 玩家来源收益排行榜
    private final String SHARE_PROMOTE_REWARDS_INCOME = "activity:sharepromote:income:%d";       // 玩家可领取收益
    private final String SHARE_PROMOTE_REWARDS_HISTORY_INCOME = "activity:sharepromote:historyincome:%d"; // 玩家历史总收益
    private final String SHARE_PROMOTE_PLAYER_INFO = "activity:sharepromote:player:%d";           // 玩家信息
    private final String SHARE_PROMOTE_LOCK = "activity:sharepromote:lock:%d";                    // 玩家操作锁

    public SharePromoteDao(RedisTemplate<String, String> redisTemplate, RedisTemplate<String, SharePromotePlayerData> playerDataRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.playerDataRedisTemplate = playerDataRedisTemplate;
    }

    /** 获取 Hash 操作对象 */
    public HashOperations<String, String, String> getHashOperations() {
        return redisTemplate.opsForHash();
    }

    /** 获取玩家操作锁 Key */
    public String getLock(long playerId) {
        return SHARE_PROMOTE_LOCK.formatted(playerId);
    }

    /**
     * 获取总收益排行榜（分页）
     *
     * @param startIndex 起始索引
     * @param size 页大小
     * @return Pair<排行榜数据, 是否有下一页>
     */
    public Pair<Map<Long, Double>, Boolean> getAllIncomeRank(int startIndex, int size) {
        return getTopIncomePlayers(SHARE_PROMOTE_REWARDS_RANK, startIndex, size);
    }

    /** 更新总收益排行榜分数 */
    public void updateRankScore(long playerId, long rank) {
        redisTemplate.opsForZSet().incrementScore(SHARE_PROMOTE_REWARDS_RANK, String.valueOf(playerId), rank);
    }

    /** 删除总收益排行榜数据 */
    public void deleteRank() {
        redisTemplate.delete(SHARE_PROMOTE_REWARDS_RANK);
    }

    /** 获取玩家是否已被绑定信息 */
    public String getBindInfo(long sourcePlayerId) {
        return redisTemplate.opsForValue().get(SHARE_PROMOTE_ALREADY_BIND_KEY.formatted(sourcePlayerId));
    }

    /**
     * 增加玩家收益
     * 会更新：
     * 1. 可领取总收益
     * 2. 历史总收益
     * 3. 当日收益
     * 4. 来源排行榜
     */
    public void addPlayerIncome(long sourcePlayerId, long beneficiaryPlayerId, long addValue) {
        if (addValue <= 0) return;

        String incomeKey = SHARE_PROMOTE_REWARDS_INCOME_RANK.formatted(beneficiaryPlayerId);

        // 可领取总收入
        String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(beneficiaryPlayerId);
        redisTemplate.opsForValue().increment(key, addValue);

        // 历史总收入
        String historyKey = SHARE_PROMOTE_REWARDS_HISTORY_INCOME.formatted(beneficiaryPlayerId);
        redisTemplate.opsForValue().increment(historyKey, addValue);

        // 当日收入
        String dailyKey = String.format("activity:sharepromote:income:%s:%d",
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), beneficiaryPlayerId);
        redisTemplate.opsForValue().increment(dailyKey, addValue);
        redisTemplate.expire(dailyKey, Duration.ofDays(1));

        // 来源排行榜
        redisTemplate.opsForZSet().incrementScore(incomeKey, String.valueOf(sourcePlayerId), addValue);
    }

    /** 获取玩家昨天总收入 */
    public long getYesterdayIncome(long playerId) {
        String key = String.format("activity:sharepromote:income:%s:%d",
                LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE), playerId);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    /**
     * 获取排行榜前 N 名玩家
     *
     * @param rankKey 排行榜 Redis Key
     * @param start 起始索引
     * @param topN 页大小
     * @return Pair<排行榜数据, 是否有下一页>
     */
    public Pair<Map<Long, Double>, Boolean> getTopIncomePlayers(String rankKey, int start, int topN) {
        Set<ZSetOperations.TypedTuple<String>> results =
                redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, start, topN);

        if (results == null || results.isEmpty()) {
            return Pair.newPair(Collections.emptyMap(), false);
        }

        LinkedHashMap<Long, Double> topPlayers = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : results) {
            if (tuple.getValue() != null) {
                topPlayers.put(Long.valueOf(tuple.getValue()), tuple.getScore());
            }
        }

        boolean hasNext = false;
        int pageSize = topN - start;
        if (topPlayers.size() > pageSize) {
            hasNext = true;
            topPlayers.pollLastEntry();
        }
        return Pair.newPair(topPlayers, hasNext);
    }

    /** 获取指定玩家的来源收益排行榜（分页） */
    public Pair<Map<Long, Double>, Boolean> getAllIncomeRank(long playerId, int startIndex, int size) {
        String incomeKey = SHARE_PROMOTE_REWARDS_INCOME_RANK.formatted(playerId);
        return getTopIncomePlayers(incomeKey, startIndex, size);
    }

    /** 删除玩家可领取收益 */
    public void delPlayerIncome(long playerId) {
        String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(playerId);
        redisTemplate.delete(key);
    }

    /** 获取玩家可领取收益 */
    public long getPlayerIncome(long playerId) {
        String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(playerId);
        String income = redisTemplate.opsForValue().get(key);
        return income == null ? 0 : Long.parseLong(income);
    }

    /** 获取玩家历史总收益 */
    public long getPlayerHistoryIncome(long playerId) {
        String key = SHARE_PROMOTE_REWARDS_HISTORY_INCOME.formatted(playerId);
        String income = redisTemplate.opsForValue().get(key);
        return income == null ? 0 : Long.parseLong(income);
    }

    /**
     * 玩家绑定推广码
     *
     * @param playerId 请求绑定的玩家
     * @param code 推广码
     * @return Code.SUCCESS/Code.ALREADY_BOUND/Code.ALREADY_OTHER_BOUND/Code.CODE_ERROR
     */
    public int bindPlayer(long playerId, String code) {
        try {
            String createPlayerId = getHashOperations().get(SHARE_PROMOTE_CODE, code);
            if (createPlayerId == null) return Code.CODE_ERROR;

            // 被绑定玩家是否已绑定过
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    SHARE_PROMOTE_ALREADY_BIND_KEY.formatted(createPlayerId), getBindString(playerId));
            if (Boolean.FALSE.equals(success)) {
                log.info("绑定失败 playerId={} 已经被绑定", playerId);
                return Code.ALREADY_OTHER_BOUND;
            }

            String bindKey = SHARE_PROMOTE_BIND_KEY.formatted(createPlayerId);
            Boolean member = redisTemplate.opsForSet().isMember(bindKey, String.valueOf(playerId));
            if (Boolean.TRUE.equals(member)) return Code.ALREADY_BOUND;

            // 保存绑定关系
            String requestBindKey = SHARE_PROMOTE_BIND_KEY.formatted(playerId);
            redisTemplate.opsForSet().add(requestBindKey, createPlayerId);

            log.info("绑定成功 playerId={} code={}", playerId, code);

            // 初始化来源排行榜
            String incomeKey = SHARE_PROMOTE_REWARDS_INCOME_RANK.formatted(playerId);
            redisTemplate.opsForZSet().add(incomeKey, createPlayerId, 0);

        } catch (Exception e) {
            log.error("绑定玩家出现异常 playerId={} code={}", playerId, code, e);
        }
        return Code.SUCCESS;
    }

    /** 构建绑定信息字符串 */
    private String getBindString(long playerId) {
        return String.format("%d_%d", playerId, System.currentTimeMillis());
    }

    /** 获取玩家已绑定的目标数量 */
    public long getBindCount(long playerId) {
        String bindKey = SHARE_PROMOTE_BIND_KEY.formatted(playerId);
        Long size = redisTemplate.opsForSet().size(bindKey);
        return size != null ? size : 0L;
    }

    /**
     * 生成全局唯一邀请码
     * 12位随机大小写字母 + 数字
     */
    public String generateUniqueCode(long playerId) {
        String code;
        boolean added;
        String strPlayerId = String.valueOf(playerId);

        do {
            code = generateCode();
            added = redisTemplate.opsForHash().putIfAbsent(SHARE_PROMOTE_CODE, code, strPlayerId);
        } while (!added);

        return code;
    }

    /** 获取玩家信息对象 */
    public SharePromotePlayerData getPlayerInfoData(long playerId) {
        String key = SHARE_PROMOTE_PLAYER_INFO.formatted(playerId);
        return playerDataRedisTemplate.opsForValue().get(key);
    }

    /** 保存玩家信息对象 */
    public void savePlayerInfoData(long playerId, SharePromotePlayerData data) {
        String key = SHARE_PROMOTE_PLAYER_INFO.formatted(playerId);
        playerDataRedisTemplate.opsForValue().set(key, data);
    }

    /** 生成随机 12 位码 */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHAR_POOL.length());
            sb.append(CHAR_POOL.charAt(index));
        }
        return sb.toString();
    }
}
