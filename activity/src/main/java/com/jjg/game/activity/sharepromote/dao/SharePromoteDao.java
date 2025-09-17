package com.jjg.game.activity.sharepromote.dao;

import com.jjg.game.activity.sharepromote.data.SharePromotePlayerData;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import org.apache.commons.lang3.StringUtils;
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
 * @author lm
 * @date 2025/9/15 17:30
 */
@Repository
public class SharePromoteDao {

    private final Logger log = LoggerFactory.getLogger(SharePromoteDao.class);
    private final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom RANDOM = new SecureRandom();
    private final int CODE_LENGTH = 12;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, SharePromotePlayerData> playerDataRedisTemplate;
    //玩家id
    private final String SHARE_PROMOTE_BIND_KEY = "activity:sharepromote:bind:%s";
    //绑定玩家的id
    private final String SHARE_PROMOTE_ALREADY_BIND_KEY = "activity:sharepromote:alreadybind:%s";
    private final String SHARE_PROMOTE_CODE = "activity:sharepromote:code";
    private final String SHARE_PROMOTE_REWARDS_RANK = "activity:sharepromote:rank";
    private final String SHARE_PROMOTE_REWARDS_INCOME_RANK = "activity:sharepromote:incomerank:%d";
    private final String SHARE_PROMOTE_REWARDS_INCOME = "activity:sharepromote:income:%d";
    private final String SHARE_PROMOTE_REWARDS_HISTORY_INCOME = "activity:sharepromote:historyincome:%d";
    private final String SHARE_PROMOTE_PLAYER_INFO = "activity:sharepromote:player:%d";
    private final String SHARE_PROMOTE_LOCK = "activity:sharepromote:lock:%d";

    public SharePromoteDao(RedisTemplate<String, String> redisTemplate, RedisTemplate<String, SharePromotePlayerData> playerDataRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.playerDataRedisTemplate = playerDataRedisTemplate;
    }


    public HashOperations<String, String, String> getHashOperations() {
        return redisTemplate.opsForHash();
    }

    public String getLock(long playerId) {
        return SHARE_PROMOTE_LOCK.formatted(playerId);
    }


    /**
     * 获取收益排行榜总分数
     */
    public Pair<Map<Long, Double>, Boolean> getPlayerIncomeRank(int startIndex, int size) {
        return getTopIncomePlayers(SHARE_PROMOTE_REWARDS_RANK, startIndex, size);
    }

    /**
     * 更新收益排行榜总分数
     */
    public void updateRankScore(long playerId, long rank) {
        redisTemplate.opsForZSet().incrementScore(SHARE_PROMOTE_REWARDS_RANK, String.valueOf(playerId), rank);
    }

    /**
     * 删除收益排行榜数据
     */
    public void deleteRank() {
        redisTemplate.delete(SHARE_PROMOTE_REWARDS_RANK);
    }

    /**
     * 添加玩家收入
     */
    public void addPlayerIncome(long sourcePlayerId, long addValue) {
        // 获取被绑定的id
        String bindInfo = SHARE_PROMOTE_ALREADY_BIND_KEY.formatted(sourcePlayerId);
        if (StringUtils.isEmpty(bindInfo)) {
            return;
        }
        String[] bindInfoArr = StringUtils.split(bindInfo, "_");
        if (bindInfoArr.length != 2) {
            return;
        }
        long playerId = Long.parseLong(bindInfoArr[0]);

        if (addValue > 0) {
            // 1. 可领取总收入
            String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(playerId);
            redisTemplate.opsForValue().increment(key, addValue);
            // 2. 历史总收入
            String historyKey = SHARE_PROMOTE_REWARDS_HISTORY_INCOME.formatted(playerId);
            redisTemplate.opsForValue().increment(historyKey, addValue);
            // 3. 按天收入
            String dailyKey = String.format("activity:sharepromote:income:%s:%d",
                    LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), playerId);
            redisTemplate.opsForValue().increment(dailyKey, addValue);
            // 设置过期时间（例如保留 1 天）
            redisTemplate.expire(dailyKey, Duration.ofDays(1));
        }
        // 2. 来源排行榜
        String incomeKey = SHARE_PROMOTE_REWARDS_INCOME_RANK.formatted(playerId);
        redisTemplate.opsForZSet().incrementScore(incomeKey, String.valueOf(sourcePlayerId), addValue);

    }

    /**
     * 获取昨天总收入
     *
     * @param playerId 玩家id
     * @return 昨天总收入
     */
    public long getYesterdayIncome(long playerId) {
        String key = String.format("activity:sharepromote:income:%s:%d",
                LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE), playerId);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    /**
     * 获取收入排行榜前N名
     *
     * @param topN 排名前N
     * @return Map<playerId, income>
     */
    public Pair<Map<Long, Double>, Boolean> getTopIncomePlayers(String rankKey, int start, int topN) {
        Set<ZSetOperations.TypedTuple<String>> results =
                redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, start, topN);
        if (results == null || results.isEmpty()) {
            return Pair.newPair(Collections.emptyMap(), false);
        }
        LinkedHashMap<Long, Double> topPlayers = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : results) {
            if (tuple.getValue() == null) {
                continue;
            }
            topPlayers.put(Long.valueOf(tuple.getValue()), tuple.getScore());
        }
        boolean hasNext = false;
        int pageSize = topN - start;
        if ( topPlayers.size() > pageSize) {
            hasNext = true;
            // 去掉多查的那一条
            topPlayers.pollLastEntry();
        }
        return Pair.newPair(topPlayers, hasNext);
    }

    /**
     * 获取玩家自己的收益排行榜信息
     */
    public Pair<Map<Long, Double>, Boolean> getPlayerIncomeRank(long playerId, int startIndex, int size) {
        String incomeKey = SHARE_PROMOTE_REWARDS_INCOME_RANK.formatted(playerId);
        return getTopIncomePlayers(incomeKey, startIndex, size);
    }

    /**
     * 删除玩家收入
     */
    public void delPlayerIncome(long playerId) {
        String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(playerId);
        redisTemplate.delete(key);
    }

    /**
     * 获取玩家可领取收入
     */
    public long getPlayerIncome(long playerId) {
        String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(playerId);
        String income = redisTemplate.opsForValue().get(key);
        return income == null ? 0 : Long.parseLong(income);
    }

    /**
     * 获取玩家历史总收入
     */
    public long getPlayerHistoryIncome(long playerId) {
        String key = SHARE_PROMOTE_REWARDS_HISTORY_INCOME.formatted(playerId);
        String income = redisTemplate.opsForValue().get(key);
        return income == null ? 0 : Long.parseLong(income);
    }


    /**
     * 绑定玩家
     *
     * @param playerId 请求被绑定人
     * @param code     绑定码
     * @return true = 绑定成功; false = 已经被绑定过
     */
    public int bindPlayer(long playerId, String code) {
        //参考邀请码是否合法
        try {
            String createPlayerId = getHashOperations().get(SHARE_PROMOTE_CODE, code);
            if (createPlayerId == null) {
                return Code.CODE_ERROR;
            }
            Boolean success = redisTemplate.opsForValue().setIfAbsent(SHARE_PROMOTE_ALREADY_BIND_KEY.formatted(createPlayerId), getBindString(playerId));
            if (Boolean.FALSE.equals(success)) {
                log.info("绑定失败 playerId={} 已经被 绑定", playerId);
                return Code.ALREADY_OTHER_BOUND;
            }
            String bindKey = SHARE_PROMOTE_BIND_KEY.formatted(createPlayerId);
            //判断即将绑定的玩家是否已经绑定该用户
            Boolean member = redisTemplate.opsForSet().isMember(bindKey, String.valueOf(playerId));
            if (Boolean.TRUE.equals(member)) {
                return Code.ALREADY_BOUND;
            }
            String requestBingKey = SHARE_PROMOTE_BIND_KEY.formatted(playerId);
            // 保存 source -> target 的绑定关系
            redisTemplate.opsForSet().add(requestBingKey, String.valueOf(playerId));
            log.info("绑定成功 playerId={} code={}", playerId, code);
            addPlayerIncome(Long.parseLong(createPlayerId), 0);
        } catch (Exception e) {
            log.error("绑定玩家出现异常 playerId={} code={}", playerId, code, e);
        }
        return Code.SUCCESS;
    }

    private String getBindString(long playerId) {
        return String.format("%d_%d", playerId, System.currentTimeMillis());
    }

    /**
     * 获取某个 source 已经绑定的 target 数量
     *
     * @param playerId 推广者ID
     * @return 绑定数量
     */
    public long getBindCount(long playerId) {
        String bindKey = SHARE_PROMOTE_BIND_KEY.formatted(playerId);
        Long size = redisTemplate.opsForSet().size(bindKey);
        return size != null ? size : 0L;
    }

    /**
     * 生成全局唯一的12位码（大小写字母 + 数字）
     *
     * @return 唯一码
     */
    public String generateUniqueCode(long playerId) {
        String code;
        boolean added;
        String strPlayerId = String.valueOf(playerId);
        do {
            code = generateCode();
            // result == 1 表示成功新增，说明之前没有
            added = redisTemplate.opsForHash().putIfAbsent(SHARE_PROMOTE_CODE, code, strPlayerId);
        } while (!added);

        return code;
    }

    public SharePromotePlayerData getPlayerInfoData(long playerId) {
        String key = SHARE_PROMOTE_PLAYER_INFO.formatted(playerId);
        return playerDataRedisTemplate.opsForValue().get(key);
    }

    public void savePlayerInfoData(long playerId, SharePromotePlayerData data) {
        String key = SHARE_PROMOTE_PLAYER_INFO.formatted(playerId);
        playerDataRedisTemplate.opsForValue().set(key, data);
    }


    /**
     * 生成随机12位码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHAR_POOL.length());
            sb.append(CHAR_POOL.charAt(index));
        }
        return sb.toString();
    }
}
