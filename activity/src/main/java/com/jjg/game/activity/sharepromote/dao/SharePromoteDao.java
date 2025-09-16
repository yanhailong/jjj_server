package com.jjg.game.activity.sharepromote.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
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
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 12;
    private final RedisTemplate<String, String> redisTemplate;
    //玩家id
    private final String SHARE_PROMOTE_BIND_KEY = "activity:sharepromote:bind:%s";
    //绑定玩家的id
    private final String SHARE_PROMOTE_ALREADY_BIND_KEY = "activity:sharepromote:alreadybind";
    private final String UNIQUE_CODE_KEY = "activity:sharepromote:code";
    private final String SHARE_PROMOTE_REWARDS_RECORD = "activity:sharepromote:record";
    private final String SHARE_PROMOTE_REWARDS_RANK = "activity:sharepromote:rank";
    private final String SHARE_PROMOTE_REWARDS_INCOME_RANK = "activity:sharepromote:incomerank:%d";
    private final String SHARE_PROMOTE_REWARDS_INCOME = "activity:sharepromote:income:%d";

    public SharePromoteDao(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public HashOperations<String, String, String> getHashOperations() {
        return redisTemplate.opsForHash();
    }

    /**
     * 获取收益排行榜总分数
     */
    public Map<Long, Double> getPlayerIncomeRank(int startIndex, int size) {
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
    public void addPlayerIncome(long playerId, long sourcePlayerId, long addValue) {
        String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(playerId);
        String incomeKey = SHARE_PROMOTE_REWARDS_INCOME_RANK.formatted(playerId);
        redisTemplate.opsForValue().increment(key, addValue);
        redisTemplate.opsForZSet().incrementScore(incomeKey, String.valueOf(sourcePlayerId), addValue);
    }

    /**
     * 获取收入排行榜前N名
     *
     * @param topN 排名前N
     * @return Map<playerId, income>
     */
    public Map<Long, Double> getTopIncomePlayers(String rankKey, int start, int topN) {
        Set<ZSetOperations.TypedTuple<String>> results =
                redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, start, topN - 1);
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> topPlayers = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : results) {
            if (tuple.getValue() == null) {
                continue;
            }
            topPlayers.put(Long.valueOf(tuple.getValue()), tuple.getScore());
        }
        return topPlayers;
    }

    /**
     * 获取玩家自己的收益排行榜信息
     */
    public Map<Long, Double> getPlayerIncomeRank(long playerId, int startIndex, int size) {
        String incomeKey = SHARE_PROMOTE_REWARDS_INCOME_RANK.formatted(playerId);
        return getTopIncomePlayers(incomeKey, startIndex, size);
    }

    /**
     * 删除玩家收入
     */
    public void delPlayerIncome(long playerId, long addValue) {
        String key = SHARE_PROMOTE_REWARDS_INCOME.formatted(playerId);
        redisTemplate.delete(key);
    }


    /**
     * 添加推广分享领奖记录
     */
    public void addRewardsRecord(long playerId, String rewardsRecord) {
        redisTemplate.opsForList().leftPush(SHARE_PROMOTE_REWARDS_RECORD, rewardsRecord);
    }

    /**
     * 获取领奖记录
     */
    public void getRewardsRecord(int startIndex, int size) {
        redisTemplate.opsForList().range(SHARE_PROMOTE_REWARDS_RECORD, startIndex, startIndex + size);
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
        String createPlayerId = getHashOperations().get(UNIQUE_CODE_KEY, code);
        if (createPlayerId == null) {
            return 1;
        }
        Boolean success = redisTemplate.opsForValue().setIfAbsent(SHARE_PROMOTE_ALREADY_BIND_KEY, createPlayerId);
        if (Boolean.FALSE.equals(success)) {
            log.info("绑定失败 playerId={} 已经被 绑定", playerId);
            return 4;
        }
        String bindKey = SHARE_PROMOTE_BIND_KEY.formatted(createPlayerId);
        //判断即将绑定的玩家是否已经绑定改用户
        Boolean member = redisTemplate.opsForSet().isMember(bindKey, String.valueOf(playerId));
        if (Boolean.TRUE.equals(member)) {
            return 2;
        }
        String requestBingKey = SHARE_PROMOTE_BIND_KEY.formatted(playerId);
        // 保存 source -> target 的绑定关系
        redisTemplate.opsForSet().add(requestBingKey, String.valueOf(playerId));
        log.info("绑定成功 playerId={} code={}", playerId, code);
        return 0;
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
    public String generateUniqueCode() {
        String code;
        boolean added;
        do {
            code = generateCode();
            // result == 1 表示成功新增，说明之前没有
            added = redisTemplate.opsForHash().putIfAbsent(UNIQUE_CODE_KEY, code, code);
        } while (!added);

        return code;
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
