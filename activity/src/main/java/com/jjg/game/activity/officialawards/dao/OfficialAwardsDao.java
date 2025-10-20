package com.jjg.game.activity.officialawards.dao;

import com.jjg.game.activity.common.dao.RecordDao;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.officialawards.data.OfficialAwardsRecord;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 官方奖励活动 DAO
 *
 * @author lm
 * @date 2025/9/22 16:53
 */
@Repository
public class OfficialAwardsDao {
    private final Logger log = LoggerFactory.getLogger(OfficialAwardsDao.class);
    // -------------------- Redis Key 定义 --------------------
    private final String FUNCTION_NAME = "officialawards";
    private final String PREFIX = "activity:" + FUNCTION_NAME;
    // 玩家进度 key: 玩家id
    private final String PLAYER_PROGRESS_KEY = PREFIX + ":player:%d";
    private final String TOTAL_POOL_KEY = PREFIX + ":pool:%s";
    private final RedisTemplate<String, String> redisTemplate;
    private final RecordDao recordDao;
    private final RedisLock redisLock;
    private final RedisUtils redisUtils;

    public OfficialAwardsDao(RedisTemplate<String, String> redisTemplate, RecordDao recordDao, RedisLock redisLock, RedisUtils redisUtils) {
        this.redisTemplate = redisTemplate;
        this.recordDao = recordDao;
        this.redisLock = redisLock;
        this.redisUtils = redisUtils;
    }

    // -------------------- 单个玩家记录 --------------------

    /**
     * 保存玩家记录
     */
    public void savePlayerRecord(long playerId, long activityId, OfficialAwardsRecord record) {
        recordDao.addRecord(FUNCTION_NAME, activityId, playerId, record, ActivityConstant.OfficialAwards.MAX_RECORD_NUM, true);
    }


    /**
     * 获取玩家记录
     */
    public Pair<Boolean, List<OfficialAwardsRecord>> getPlayerRecord(long playerId, long activityId, int start, int end) {
        int mined = Math.min(ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM, end);
        return recordDao.getPlayerRecords(FUNCTION_NAME, activityId, playerId, start, mined, OfficialAwardsRecord.class);
    }

    /**
     * 删除活动所有玩家记录
     */
    public void deleteAllPlayerRecords(long activityId) {
        recordDao.deleteAllPlayerRecords(FUNCTION_NAME, activityId);
    }

    // -------------------- 全部玩家记录 --------------------

    /**
     * 获取活动全部玩家记录
     */
    public Pair<Boolean, List<OfficialAwardsRecord>> getAllRecords(long activityId,int start, int end) {
        int mined = Math.min(ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM, end);
        return recordDao.getRecords(FUNCTION_NAME, activityId, start, mined, OfficialAwardsRecord.class);
    }

    /**
     * 删除活动所有全局记录
     */
    public void deleteAllRecords(long activityId) {
        recordDao.deleteAllRecords(FUNCTION_NAME, activityId);
    }

    // -------------------- 进度 --------------------


    /**
     * 获取玩家进度
     */
    public int getPlayerProgress(long playerId) {
        String key = String.format(PLAYER_PROGRESS_KEY, playerId);
        String progress = redisTemplate.opsForValue().get(key);
        return progress == null ? 0 : Integer.parseInt(progress);
    }

    /**
     * 扣除玩家进度
     */
    public int reducePlayerProgress(long playerId, int reduceValue) {
        String key = String.format(PLAYER_PROGRESS_KEY, playerId);
        return reduceProgress(key, reduceValue);
    }

    public int reduceProgress(String redisKey, int reduceValue) {
        String lockKey = "lock:" + redisKey;
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            String progress = redisTemplate.opsForValue().get(redisKey);
            int currentProgress = progress == null ? 0 : Integer.parseInt(progress);
            int remain = currentProgress - reduceValue;
            if (remain < 0) {
                return -1;
            }
            redisTemplate.opsForValue().set(redisKey, String.valueOf(remain));
            return remain;
        } catch (Exception e) {
            log.error("reduce player progress", e);
        } finally {
            redisLock.unlock(lockKey);
        }
        return -1;
    }

    /**
     * 删除玩家进度
     *
     * @param playerId 玩家id
     */
    public void deletePlayerProgress(long playerId) {
        String key = String.format(PLAYER_PROGRESS_KEY, playerId);
        redisTemplate.delete(key);
    }

    /**
     * 玩家进度自增 (积分累加)
     */
    public int incrementPlayerProgress(long playerId, long delta) {
        String key = String.format(PLAYER_PROGRESS_KEY, playerId);
        Long increment = redisTemplate.opsForValue().increment(key, delta);
        return increment == null ? 0 : increment.intValue();
    }

    /**
     * 删除所有玩家的所有进度
     */
    public void deleteAllPlayerAllProgress() {
        redisUtils.deleteByPattern(redisTemplate, PREFIX + ":player:*");
    }
// -------------------- 全局奖池 --------------------

    /**
     * 设置奖池数量
     */
    public void setTotalPool(long activityId, long delta) {
        redisTemplate.opsForValue().set(TOTAL_POOL_KEY.formatted(activityId), String.valueOf(delta));
    }

    /**
     * 获取奖池数量
     */
    public long getTotalPool(long activityId) {
        String value = redisTemplate.opsForValue().get(TOTAL_POOL_KEY.formatted(activityId));
        return value == null ? 0 : Long.parseLong(value);
    }

    /**
     * 扣除奖池数量（扣减数量 剩余数量）
     *
     * @return 扣减数量 剩余数量
     */
    public Pair<Integer, Integer> reduceTotalPool(long activityId, int reduceValue) {
        String key = TOTAL_POOL_KEY.formatted(activityId);
        String lockKey = "lock:" + key;
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            String val = redisTemplate.opsForValue().get(key);
            int current = val == null ? 0 : Integer.parseInt(val);
            if (current == 0) {
                return null;
            }
            reduceValue = Math.min(reduceValue, current);
            int remain = current - reduceValue;
            redisTemplate.opsForValue().set(key, String.valueOf(remain));
            return Pair.newPair(reduceValue, remain);
        } catch (Exception e) {
            log.error("reduce total pool error", e);
        } finally {
            redisLock.unlock(lockKey);
        }
        return null;
    }

    /**
     * 删除奖池
     */
    public void deleteTotalPool(long activityId) {
        redisTemplate.delete(TOTAL_POOL_KEY.formatted(activityId));
    }
}
