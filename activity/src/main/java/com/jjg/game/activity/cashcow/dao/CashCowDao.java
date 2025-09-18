package com.jjg.game.activity.cashcow.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.cashcow.data.CashCowRecordData;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.proto.Pair;
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
import java.util.Map;

/**
 * 摇钱树 DAO
 * <p>
 * 主要负责摇钱树相关数据的 Redis 存取，包括：
 * - 玩家免费奖励状态
 * - 玩家进度
 * - 活动奖池（总奖池 & 分奖池）
 * - 活动记录（个人 & 全局）
 * <p>
 * 使用 Redis 实现高性能缓存和并发安全（通过分布式锁 RedisLock）。
 *
 * @author lm
 * @date 2025/9/9 18:15
 */
@Repository
public class CashCowDao {
    private final Logger log = LoggerFactory.getLogger(CashCowDao.class);

    /**
     * 玩家/全局记录存储，key->List，value->CashCowRecordData
     */
    private final RedisTemplate<String, CashCowRecordData> recordRedisTemplate;
    /**
     * 长整型/字符串数据存储，key->String，value->String
     */
    private final RedisTemplate<String, String> longRedisTemplate;
    /**
     * Redis 分布式锁，保证并发安全
     */
    private final RedisLock lock;

    // -------------------- Redis Key 定义 --------------------
    private final String PLAYER_RECORD_KEY = "activity:cashcow:record:%d:%d";   // 单个玩家的活动记录 key
    private final String ALL_RECORD_KEY = "activity:cashcow:record:all:%d";    // 全部玩家的活动记录 key
    private final String POOL_KEY = "activity:cashcow:poll:%d";                // 活动奖池 key（Hash）
    private final String POOL_LOCK_KEY = "activity:cashcow:polllock:%d:%d";    // 活动奖池锁 key
    private final String PLAYER_PROGRESS_KEY = "activity:cashcow:player:%d:%d";// 玩家进度 key
    private final String PLAYER_FREE_KEY = "activity:cashcow:free:%d:%d";      // 玩家免费奖励 key
    private final String PLAYER_FREE_LOCK_KEY = "activity:cashcow:freelock:%d:%d"; // 玩家免费奖励锁 key


    public CashCowDao(RedisTemplate<String, CashCowRecordData> recordRedisTemplate, RedisTemplate<String, String> longRedisTemplate, RedisLock lock) {
        this.recordRedisTemplate = recordRedisTemplate;
        this.longRedisTemplate = longRedisTemplate;
        this.lock = lock;
    }


    // -------------------- 免费奖励相关 --------------------

    /**
     * 获取玩家是否已经领取当天的免费奖励
     *
     * @param playerId   玩家id
     * @param activityId 活动id
     * @return true 已领取，false 未领取
     */
    public boolean getFreeRewardsStatus(long playerId, long activityId) {
        String lastTime = longRedisTemplate.opsForValue().get(PLAYER_FREE_KEY.formatted(playerId, activityId));
        if (lastTime == null) {
            return false;
        }
        // 判断是否在同一天（同一天 -> 已领取）
        return TimeHelper.inSameDay(Long.parseLong(lastTime), System.currentTimeMillis());
    }

    /**
     * 获取玩家免费奖励锁 key
     */
    public String getPlayerFreeLockKey(long playerId, long activityId) {
        return String.format(PLAYER_FREE_LOCK_KEY, playerId, activityId);
    }

    /**
     * 记录玩家领取免费奖励的时间
     * <p>
     * 存储当前时间戳（通过业务逻辑判断 inSameDay）。
     */
    public void addFreeRewardsCount(long playerId, long activityId) {
        longRedisTemplate.opsForValue().set(PLAYER_FREE_KEY.formatted(playerId, activityId), String.valueOf(System.currentTimeMillis()));
    }


    // -------------------- 玩家进度相关 --------------------

    /**
     * 获取玩家在某个活动中的进度
     *
     * @return 当前进度，默认为 0
     */
    public long getPlayerActivityProgress(long playerId, long activityId) {
        String playerProgressKey = String.format(PLAYER_PROGRESS_KEY, playerId, activityId);
        String progress = longRedisTemplate.opsForValue().get(playerProgressKey);
        return progress == null ? 0 : Long.parseLong(progress);
    }

    /**
     * 增加玩家进度
     *
     * @return 增加后的进度总值
     */
    public long addPlayerActivityProgress(long playerId, long activityId, long addValue) {
        String playerProgressKey = String.format(PLAYER_PROGRESS_KEY, playerId, activityId);
        Long progress = longRedisTemplate.opsForValue().increment(playerProgressKey, addValue);
        return progress == null ? 0 : progress;
    }

    /**
     * 删除玩家进度
     * - 主要用于领奖后清理
     */
    public void delPlayerActivityProgress(long playerId, long activityId) {
        String playerProgressKey = String.format(PLAYER_PROGRESS_KEY, playerId, activityId);
        longRedisTemplate.delete(playerProgressKey);
    }


    // -------------------- 活动奖池相关 --------------------

    /**
     * 获取指定 detailId 的活动奖池余额
     */
    public long getSpecifiedActivityPool(long activityId, int detailId) {
        String poolKey = String.format(POOL_KEY, activityId);
        HashOperations<String, String, String> hash = getOpsForHash();
        String pool = hash.get(poolKey, String.valueOf(detailId));
        return pool == null ? 0 : Long.parseLong(pool);
    }

    /**
     * 获取活动的总奖池（所有 detailId 的和）
     *
     * @return 总奖池值
     */
    public long getActivityPool(long activityId) {
       return getActivityPool(activityId,0).getFirst();
    }

    /**
     * 获取活动的总奖池（所有 detailId 的和）和指定池的值
     *
     * @return 总奖池值, 指定奖池值
     */
    public Pair<Long, Long> getActivityPool(long activityId, int detailId) {
        String poolKey = String.format(POOL_KEY, activityId);
        HashOperations<String, String, String> hash = getOpsForHash();
        Map<String, String> values = hash.entries(poolKey);
        if (CollectionUtil.isEmpty(values)) {
            return Pair.newPair(0L, 0L);
        }
        long totalPool = 0;
        long pool = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            long temp = Long.parseLong(entry.getValue());
            if (Integer.parseInt(entry.getKey()) == detailId) {
                pool = temp;
            }
            totalPool += temp;
        }
        return Pair.newPair(totalPool, pool);
    }

    /**
     * 设置某个 detailId 的奖池值
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
     * 增加某个 detailId 的奖池值
     *
     * @return 增加后的总值
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
     * 按比例减少奖池（分配奖励）
     *
     * @param distribution 扣减比例（万分比，例如 2000 = 20%）
     * @return 扣减后剩余的奖池数量
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
            // 计算剩余奖池：realPool - (realPool * distribution / 10000)
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


    // -------------------- 活动记录相关 --------------------

    /**
     * 保存一条玩家活动记录
     * - 存入玩家个人日志
     * - 同时存入全局日志
     */
    public void savePlayerRecordActivity(long playerId, long activityId, CashCowRecordData data) {
        try {
            String playerKey = String.format(PLAYER_RECORD_KEY, activityId, playerId);
            // 玩家个人记录（List 左进）
            recordRedisTemplate.opsForList().leftPush(playerKey, data);
            // 全局记录（List 左进）
            recordRedisTemplate.opsForList().leftPush(String.format(ALL_RECORD_KEY, activityId), data);
        } catch (Exception e) {
            log.error("保存玩家活动记录失败");
        }
    }

    /**
     * 获取指定玩家的活动记录（分页）
     *
     * @param start 起始下标
     * @param end   结束下标（包含）
     * @return Pair(记录列表, 是否有下一页)
     */
    public Pair<List<CashCowRecordData>, Boolean> getPlayerRecordActivities(long playerId, long activityId, int start, int end) {
        String playerKey = String.format(PLAYER_RECORD_KEY, activityId, playerId);
        List<CashCowRecordData> records = recordRedisTemplate.opsForList().range(playerKey, start, end);
        return getRecordData(start, end, records);
    }

    /**
     * 获取所有玩家的活动记录（分页）
     */
    public Pair<List<CashCowRecordData>, Boolean> getAllRecordActivities(long activityId, int start, int end) {
        List<CashCowRecordData> records = recordRedisTemplate.opsForList().range(String.format(ALL_RECORD_KEY, activityId), start, end);
        return getRecordData(start, end, records);
    }

    /**
     * 公共分页处理逻辑
     *
     * @param start   起始下标
     * @param end     结束下标
     * @param records 查询结果
     * @return Pair(实际返回的数据, 是否有下一页)
     */
    private Pair<List<CashCowRecordData>, Boolean> getRecordData(int start, int end, List<CashCowRecordData> records) {
        boolean hasNext = false;
        int pageSize = end - start;
        if (records != null && records.size() > pageSize) {
            hasNext = true;
            // 去掉多查的那一条，避免超量
            records = records.subList(0, pageSize);
        }
        return Pair.newPair(records, hasNext);
    }
}
