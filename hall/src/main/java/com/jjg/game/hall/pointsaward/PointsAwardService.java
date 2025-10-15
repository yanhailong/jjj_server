package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.leaderboard.PointsAwardLeaderboardService;
import com.jjg.game.hall.pointsaward.pb.res.NotifySyncPlayerPoint;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 积分大奖积分服务
 */
@Service
public class PointsAwardService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedissonClient redissonClient;
    private final PointsAwardLeaderboardService leaderboardService;
    private final RedisLock redisLock;
    private final ClusterSystem clusterSystem;

    public PointsAwardService(RedissonClient redissonClient,
                              @Lazy ClusterSystem clusterSystem,
                              PointsAwardLeaderboardService leaderboardService,
                              RedisLock redisLock) {
        this.redissonClient = redissonClient;
        this.clusterSystem = clusterSystem;
        this.leaderboardService = leaderboardService;
        this.redisLock = redisLock;
    }

    /**
     * 积分数据锁
     */
    public String lockKey(long playerId) {
        return PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK + playerId;
    }

    /**
     * 积分key
     */
    private String atomicKey(long playerId) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_DATA_POINTS + playerId;
    }

    /**
     * 添加积分
     *
     * @param playerId    玩家id
     * @param pointsAward 增加的积分
     */
    public void add(long playerId, int pointsAward) {
        if (pointsAward <= 0) {
            return;
        }
        try {
            redisLock.lockAndRun(lockKey(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> addWithoutLock(playerId, pointsAward));
        } catch (Exception e) {
            log.error("add 玩家积分更新失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
        }
    }

    /**
     * 添加积分
     *
     * @param playerId    玩家id
     * @param pointsAward 增加的积分
     */
    public void addWithoutLock(long playerId, int pointsAward) {
        if (pointsAward <= 0) {
            return;
        }
        try {
            RAtomicLong counter = redissonClient.getAtomicLong(atomicKey(playerId));
            long currentPoints = counter.get();
            updatePointsWithOverflowProtection(counter, currentPoints, pointsAward);
            //通知玩家同步分数
            noticeSyncPoints(playerId, counter.get());
        } catch (Exception e) {
            log.error("add 玩家积分更新失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
        }
        // 排行榜更新
        updateLeaderboards(playerId, pointsAward);
    }

    /**
     * 更新积分,带溢出保护
     *
     * @param counter       Redis原子计数器
     * @param currentPoints 当前积分值
     * @param pointsToAdd   要增加的积分
     */
    private void updatePointsWithOverflowProtection(RAtomicLong counter, long currentPoints, int pointsToAdd) {
        // 检查是否会溢出
        if (Long.MAX_VALUE - currentPoints < (long) pointsToAdd) {
            setToMaxValue(counter);
        } else {
            counter.addAndGet(pointsToAdd);
        }
    }

    /**
     * 在条件验证下将积分设置为最大值
     *
     * @param counter Redis原子计数器
     */
    private void setToMaxValue(RAtomicLong counter) {
        long target = Long.MAX_VALUE;
        while (true) {
            long curr = counter.get();
            if (counter.compareAndSet(curr, target)) {
                break;
            }
        }
    }

    /**
     * 更新排行榜
     *
     * @param playerId    玩家ID
     * @param pointsAward 增加的积分
     */
    private void updateLeaderboards(long playerId, int pointsAward) {
        try {
            long nowTs = System.currentTimeMillis();
            // 更新日榜
            leaderboardService.upsert(playerId, pointsAward, nowTs);
            // 同步更新月榜(用于每月结算快照)
            leaderboardService.upsert(PointsAwardConstant.Leaderboard.TYPE_MONTH, playerId, pointsAward, nowTs);
        } catch (Exception e) {
            log.warn("更新排行榜失败 playerId=[{}], points=[{}]", playerId, pointsAward, e);
        }
    }

    /**
     * 扣除积分
     *
     * @param playerId    玩家id
     * @param pointsAward 扣除的积分 只支持正数
     */
    public boolean deduct(long playerId, int pointsAward, Predicate<Long> guard) {
        if (pointsAward <= 0) {
            return false;
        }
        try {
            // 使用分布式锁，确保判断与扣减原子执行
            return redisLock.lockAndGet(lockKey(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> deductWithoutLock(playerId, pointsAward));
        } catch (Exception e) {
            log.error("deduct 玩家积分扣减失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
            return false;
        }
    }

    /**
     * 在已持有外部锁的场景下扣除积分（不再重复加锁）
     * <p>
     * 仅在调用方已持有同一玩家的积分数据锁（如：{@link #lockKey(long)} 对应的锁）时使用，
     * 以避免锁的嵌套导致的风险。内部使用 CAS 保障原子扣减。
     *
     * @param playerId    玩家id
     * @param pointsAward 扣除的积分 只支持正数
     * @return true 扣减成功；false 扣减失败（余额不足或 CAS 冲突未能成功）
     */
    public boolean deductWithoutLock(long playerId, int pointsAward) {
        if (pointsAward <= 0) {
            return false;
        }
        try {
            RAtomicLong counter = redissonClient.getAtomicLong(atomicKey(playerId));
            while (true) {
                long curr = counter.get();
                if (curr < pointsAward) {
                    return false;
                }
                long next = curr - pointsAward;
                if (counter.compareAndSet(curr, next)) {
                    //通知玩家同步分数
                    noticeSyncPoints(playerId, next);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("deductWithinLock 玩家积分扣减失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
            return false;
        }
    }

    /**
     * 已玩家积分锁来进行扣除操作 外部不需要加锁 强制使用{@link #lockKey(long)}锁处理外部逻辑
     *
     * @param playerId    玩家id
     * @param pointsAward 扣除的积分数量 只能是正数
     * @param supplier    外部验证逻辑
     * @param success     验证通过并且扣除成功后执行的逻辑
     */
    public <T> T deduct(long playerId, int pointsAward, Supplier<Boolean> supplier, Supplier<T> success, T defaultValue) {
        return lockAndGet(playerId, () -> {
            boolean result = supplier.get();
            if (result) {
                boolean deducted = deductWithoutLock(playerId, pointsAward);
                if (deducted) {
                    return success.get();
                }
            }
            return defaultValue;
        });
    }

    /**
     * 玩家积分大奖逻辑执行方法 使用同一个锁
     *
     * @param playerId 玩家id
     * @param runnable 需要加锁的逻辑
     */
    public void lockAndRun(long playerId, Runnable runnable) {
        redisLock.lockAndRun(lockKey(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, runnable);
    }

    /**
     * 使用分布式锁获取玩家积分数据的方法。
     *
     * @param playerId 玩家ID，用于标识需要加锁的目标玩家。
     * @param supplier 需要在加锁后执行的逻辑，返回泛型类型结果。
     * @return 通过supplier执行后的结果。
     */
    public <T> T lockAndGet(long playerId, Supplier<T> supplier) {
        return redisLock.lockAndGet(lockKey(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, supplier);
    }

    /**
     * 获取玩家积分
     */
    public long getPoints(long playerId) {
        return redissonClient.getAtomicLong(atomicKey(playerId)).get();
    }

    /**
     * 通知玩家同步积分
     *
     * @param player 玩家id
     * @param points 积分
     */
    public void noticeSyncPoints(long player, long points) {
        NotifySyncPlayerPoint syncPlayerPoint = new NotifySyncPlayerPoint();
        syncPlayerPoint.setPoint(points);
        PFSession pfSession = clusterSystem.getSession(player);
        if (pfSession != null) {
            pfSession.send(syncPlayerPoint);
        }
    }

}
