package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.db.PointsAwardData;
import com.jjg.game.hall.pointsaward.db.PointsAwardDataDao;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 玩家积分大奖积分服务
 * <p>
 * <span style="color:red">修改积分值,只能通过{@link PlayerPointsAwardService#add(long, int, Consumer)}或者{@link PlayerPointsAwardService#deduct(long, int, Consumer)}</span>
 */
@Service
public class PlayerPointsAwardService implements IPlayerLoginSuccess {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedisLock redisLock;
    private final RedissonClient redissonClient;
    private final PointsAwardDataDao pointsAwardDataDao;

    /**
     * 玩家数据缓存map
     */
    private RMap<Long, PointsAwardData> cacheMap;

    /**
     * 锁持有时间（毫秒）
     */
    private static final int LOCK_LEASE_MILLIS = 2000;

    public PlayerPointsAwardService(RedisLock redisLock, RedissonClient redissonClient, PointsAwardDataDao pointsAwardDataDao) {
        this.redisLock = redisLock;
        this.redissonClient = redissonClient;
        this.pointsAwardDataDao = pointsAwardDataDao;
    }

    /**
     * 玩家登录成功事件
     *
     * @param playerController 玩家信息
     * @param player
     * @param firstLogin       是否是首次登录
     * @return true 继续执行 false终止执行
     */
    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, boolean firstLogin) {
        //加载玩家积分大奖数据 更新的时候会自动从缓存获取 没有则新建并缓存
        updateWithLock(player.getId(), null, null);
    }

    /**
     * 积分数据锁
     */
    public String lockKey(long playerId) {
        return PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK + playerId;
    }

    private RMap<Long, PointsAwardData> getCacheMap() {
        if (cacheMap == null) {
            cacheMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_DATA);
        }
        return cacheMap;
    }

    /**
     * 积分key
     */
    private String atomicKey(long playerId) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_DATA_POINTS + playerId;
    }

    /**
     * 加载玩家积分大奖活动数据
     *
     * @param playerId 玩家id
     */
    private PointsAwardData loadData(long playerId) {
        PointsAwardData data = getCacheMap().get(playerId);
        if (data == null) {
            data = pointsAwardDataDao.findByPlayerId(playerId);
            if (data == null) {
                data = new PointsAwardData();
                data.setPlayerId(playerId);
            }
        }
        return data;
    }

    /**
     * 更新玩家积分大奖数据，该方法会在操作对象时加锁以确保线程安全。
     *
     * @param playerId       玩家ID，用于标识唯一玩家。
     * @param pointsOverride 可选的积分覆盖值，如果不为null则优先采用该值覆盖原有积分。
     * @param mutator        数据修改操作，用于对PointsAwardData对象执行特定字段的修改操作。
     */
    private void updateWithLock(long playerId, Long pointsOverride, Consumer<PointsAwardData> mutator) {
        PointsAwardData awardData = redisLock.lockAndGet(lockKey(playerId), LOCK_LEASE_MILLIS, () -> {
            PointsAwardData data = loadData(playerId);
            try {
                long pointsVal = pointsOverride != null ? pointsOverride : redissonClient.getAtomicLong(atomicKey(playerId)).get();
                //始终覆盖为内存中的原子积分
                data.setPoints(pointsVal);
                if (mutator != null) {
                    mutator.accept(data);
                }
                getCacheMap().fastPut(playerId, data);
                return data;
            } catch (Exception e) {
                log.error("updateObjectWithLock 更新对象失败! playerId = [{}]", playerId, e);
            }
            return null;
        });
        if (awardData != null) {
            pointsAwardDataDao.save(awardData);
        }
    }

    /**
     * 更新玩家积分大奖数据，该方法会在操作对象时加锁以确保线程安全。
     * <p>
     * <span style="color:red"> 会自动更新积分为缓存中的原子积分值,
     * 有积分操作先调用方法{@link PlayerPointsAwardService#add(long, int, Consumer)}增加</span>
     *
     * @param playerId 玩家ID，用于标识唯一玩家。
     * @param mutator  数据修改操作，用于对PointsAwardData对象执行特定字段的修改操作。
     */
    public void updateWithLock(long playerId, Consumer<PointsAwardData> mutator) {
        updateWithLock(playerId, null, mutator);
    }

    /**
     * 添加积分
     *
     * @param playerId    玩家id
     * @param pointsAward 增加的积分
     * @param mutator     额外的其他操作 不能操作积分
     */
    public void add(long playerId, int pointsAward, Consumer<PointsAwardData> mutator) {
        if (pointsAward <= 0) {
            return;
        }
        RAtomicLong counter = redissonClient.getAtomicLong(atomicKey(playerId));
        long newVal;
        try {
            //如果接近 Long.MAX_VALUE，截断为最大值
            long before = counter.get();
            if (Long.MAX_VALUE - before < (long) pointsAward) {
                // 将值设置为 Long.MAX_VALUE
                long target = Long.MAX_VALUE;
                //可能存在并发修改
                while (true) {
                    long curr = counter.get();
                    if (curr == target) {
                        newVal = curr;
                        break;
                    }
                    //修改玩家积分 只有获取到最新的积分才覆盖避免并发修改
                    if (counter.compareAndSet(curr, target)) {
                        newVal = target;
                        break;
                    }
                }
            } else {
                newVal = counter.addAndGet(pointsAward);
            }
        } catch (Exception e) {
            log.error("add 玩家积分更新失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
            return;
        }
        // 覆盖最新积分，更新缓存并保存DB
        updateWithLock(playerId, newVal, mutator);
    }

    /**
     * 扣除积分
     *
     * @param playerId    玩家id
     * @param pointsAward 扣除的积分 只支持正数
     * @param mutator     额外的其他操作 不能操作积分
     */
    public boolean deduct(long playerId, int pointsAward, Consumer<PointsAwardData> mutator) {
        if (pointsAward <= 0) {
            return false;
        }
        RAtomicLong counter = redissonClient.getAtomicLong(atomicKey(playerId));
        long newVal;
        try {
            while (true) {
                long curr = counter.get();
                if (curr < pointsAward) {
                    return false;
                }
                long next = curr - pointsAward;
                if (counter.compareAndSet(curr, next)) {
                    newVal = next;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("deduct 玩家积分扣减失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
            return false;
        }
        //覆盖最新积分，更新缓存并保存DB
        updateWithLock(playerId, newVal, mutator);
        return true;
    }

    /**
     * 获取玩家积分大奖对象
     *
     * @param playerId 玩家id
     * @return 存在null值
     */
    public PointsAwardData getPlayerData(long playerId) {
        return getCacheMap().get(playerId);
    }

}
