package com.jjg.game.activity.cashcow.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.cashcow.data.CashCowRecordData;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author lm
 * @date 2025/9/9 18:15
 */
@Repository
public class CashCowDao {
    private final Logger log = LoggerFactory.getLogger(CashCowDao.class);
    private final RedisTemplate<String, CashCowRecordData> recordRedisTemplate;
    private final RedisTemplate<String, String> longRedisTemplate;
    //redis 锁
    private final RedisLock lock;
    private final String PLAYER_RECORD_KEY = "activity:cashcow:record:%d:%d"; // 单个玩家记录
    private final String ALL_RECORD_KEY = "activity:cashcow:record:all:%d";          // 全部玩家记录
    private final String POOL_KEY = "activity:cashcow:poll:%d";          // 总池
    private final String POOL_LOCK_KEY = "activity:cashcow:polllock:%d:%d";          // 总池锁
    private final String PLAYER_PROGRESS_KEY = "activity:cashcow:player:%d:%d";          // 玩家进度
    private final String PLAYER_FREE_KEY = "activity:cashcow:free:%d:%d";          // 免费道具
    private final String PLAYER_FREE_LOCK_KEY = "activity:cashcow:freelock:%d:%d";          // 免费道具


    public CashCowDao(RedisTemplate<String, CashCowRecordData> recordRedisTemplate, RedisTemplate<String, String> longRedisTemplate, RedisLock lock) {
        this.recordRedisTemplate = recordRedisTemplate;
        this.longRedisTemplate = longRedisTemplate;
        this.lock = lock;
    }


    /**
     * 获取免费道具领取状态
     *
     * @param playerId   玩家id
     * @param activityId 活动id
     * @return true 已经领取 false没有领取
     */
    public boolean getFreeRewardsStatus(long playerId, long activityId) {
        String lastTime = longRedisTemplate.opsForValue().get(PLAYER_FREE_KEY.formatted(playerId, activityId));
        if (lastTime == null) {
            return false;
        }
        return TimeHelper.inSameDay(Long.parseLong(lastTime), System.currentTimeMillis());
    }

    public String getPlayerFreeLockKey(long playerId, long activityId) {
        return String.format(PLAYER_FREE_LOCK_KEY, playerId, activityId);
    }

    /**
     * 到下一天凌晨自动取消
     *
     * @param playerId   玩家id
     * @param activityId 活动id
     */
    public void addFreeRewardsCount(long playerId, long activityId) {
        longRedisTemplate.opsForValue().set(PLAYER_FREE_KEY.formatted(playerId, activityId), String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 摇钱树获取玩家进度奖池
     */
    public long getPlayerActivityProgress(long playerId, long activityId) {
        String playerProgressKey = String.format(PLAYER_PROGRESS_KEY, playerId, activityId);
        String progress = longRedisTemplate.opsForValue().get(playerProgressKey);
        return progress == null ? 0 : Long.parseLong(progress);
    }

    /**
     * 摇钱树增加玩家进度奖池
     */
    public long addPlayerActivityProgress(long playerId, long activityId, long addValue) {
        String playerProgressKey = String.format(PLAYER_PROGRESS_KEY, playerId, activityId);
        Long progress = longRedisTemplate.opsForValue().increment(playerProgressKey, addValue);
        return progress == null ? 0 : progress;
    }


    /**
     * 摇钱树删除玩家进度奖池
     */
    public void delPlayerActivityProgress(long playerId, long activityId) {
        String playerProgressKey = String.format(PLAYER_PROGRESS_KEY, playerId, activityId);
        longRedisTemplate.delete(playerProgressKey);
    }


    /**
     * 摇钱树获取活动奖池
     */
    public long getActivityPool(long activityId, int detailId) {
        String poolKey = String.format(POOL_KEY, activityId);
        HashOperations<String, String, String> hash = getOpsForHash();
        String pool = hash.get(poolKey, String.valueOf(detailId));
        return pool == null ? 0 : Long.parseLong(pool);
    }

    /**
     * 摇钱树获取活动奖池
     */
    public long getActivityPool(long activityId) {
        String poolKey = String.format(POOL_KEY, activityId);
        HashOperations<String, String, String> hash = getOpsForHash();
        List<String> values = hash.values(poolKey);
        if (CollectionUtil.isEmpty(values)) {
            return 0;
        }
        return values.stream().mapToLong(Long::parseLong).sum();
    }

    /**
     * 摇钱树设置活动奖池
     */
    public void setActivityPool(long activityId, int detailId, long setValue) {
        String poolKey = String.format(POOL_KEY, activityId);
        String poolLock = String.format(POOL_LOCK_KEY, activityId, detailId);
        lock.lock(poolLock, ActivityConstant.Common.REDIS_LOCK);
        try {
            HashOperations<String, String, String> hash = getOpsForHash();
            hash.put(poolKey, String.valueOf(detailId), String.valueOf(setValue));
        } catch (Exception e) {
            log.error("设置摇钱树奖池失败 activityId:{} detailId:{} addValue:{}", activityId, detailId, activityId, e);
        } finally {
            lock.unlock(poolLock);
        }
    }

    /**
     * 摇钱树添加活动奖池
     */
    public long addActivityPool(long activityId, int detailId, long addValue) {
        String poolKey = String.format(POOL_KEY, activityId);
        String poolLock = String.format(POOL_LOCK_KEY, activityId, detailId);
        lock.lock(poolLock, ActivityConstant.Common.REDIS_LOCK);
        try {
            HashOperations<String, String, String> hash = getOpsForHash();
            String lastPool = hash.get(poolKey, String.valueOf(detailId));
            long realLastPool = lastPool == null ? 0 : Long.parseLong(lastPool);
            long total = realLastPool + addValue;
            hash.put(poolKey, String.valueOf(detailId), String.valueOf(total));
            return total;
        } catch (Exception e) {
            log.error("增加摇钱树奖池失败 activityId:{} detailId:{} addValue:{}", activityId, detailId, activityId, e);
        } finally {
            lock.unlock(poolLock);
        }
        return 0;
    }

    private HashOperations<String, String, String> getOpsForHash() {
        return longRedisTemplate.opsForHash();
    }

    /**
     * 摇钱树减少活动奖池
     *
     * @param distribution 扣减比例
     * @return 本次扣减的数量
     */
    public long reduceActivityPool(long activityId, int detailId, int distribution) {
        String poolKey = String.format(POOL_KEY, activityId);
        String poolLock = String.format(POOL_LOCK_KEY, activityId, detailId);
        lock.lock(poolLock, ActivityConstant.Common.REDIS_LOCK);
        try {
            HashOperations<String, String, String> opsForHash = getOpsForHash();
            String pool = opsForHash.get(poolKey, String.valueOf(detailId));
            if (pool == null) {
                return 0;
            }
            long realPool = Long.parseLong(pool);
            long remain = realPool - (BigDecimal.valueOf(realPool)
                    .multiply(BigDecimal.valueOf(distribution))
                    .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN))
                    .longValue();
            opsForHash.put(poolKey, String.valueOf(detailId), String.valueOf(remain));
            return remain;
        } catch (Exception e) {
            log.error("减少摇钱树奖池失败 activityId:{} detailId:{} addValue:{}", activityId, detailId, activityId, e);
        } finally {
            lock.unlock(poolLock);
        }
        return 0;
    }

    /**
     * 保存一条玩家活动记录
     */
    public void savePlayerRecordActivity(long playerId, long activityId, CashCowRecordData data) {
        try {
            String playerKey = String.format(PLAYER_RECORD_KEY, activityId, playerId);
            // 推入玩家专属日志
            recordRedisTemplate.opsForList().leftPush(playerKey, data);
            // 推入全局日志
            recordRedisTemplate.opsForList().leftPush(String.format(ALL_RECORD_KEY, activityId), data);
        } catch (Exception e) {
            log.error("保存玩家活动记录失败");
        }
    }

    /**
     * 获取指定玩家的活动记录（分页）
     */
    public List<CashCowRecordData> getPlayerRecordActivities(long playerId, long activityId, int start, int end) {
        String playerKey = String.format(PLAYER_RECORD_KEY, activityId, playerId);
        return recordRedisTemplate.opsForList().range(playerKey, start, end);
    }

    /**
     * 获取所有玩家的活动记录（分页）
     */
    public List<CashCowRecordData> getAllRecordActivities(long activityId, int start, int end) {
        return recordRedisTemplate.opsForList().range(String.format(ALL_RECORD_KEY, activityId), start, end);
    }
}
