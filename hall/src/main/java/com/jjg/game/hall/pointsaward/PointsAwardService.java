package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.rpc.RpcCallSetting;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.PointsAwardType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.rpc.HallPointsAwardBridge;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.leaderboard.PointsAwardLeaderboardService;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLadderRewardsInfo;
import com.jjg.game.hall.pointsaward.pb.res.NotifySyncPlayerPoint;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 积分大奖积分服务
 */
@Service
public class PointsAwardService implements IPlayerLoginSuccess, GmListener, HallPointsAwardBridge {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedissonClient redissonClient;
    private final PointsAwardLeaderboardService leaderboardService;
    private final RedisLock redisLock;
    private final ClusterSystem clusterSystem;
    private final MarsCurator marsCurator;
    private final PointsAwardLogger pointsAwardLogger;
    private final PlayerPackService playerPackService;

    /**
     * 初始化时间
     */
    private LocalDate initDate;

    /**
     * 玩家累计充值金额
     */
    private RMap<Long, Long> rechargeMap;

    public PointsAwardService(RedissonClient redissonClient,
                              @Lazy ClusterSystem clusterSystem,
                              PointsAwardLeaderboardService leaderboardService,
                              MarsCurator marsCurator,
                              RedisLock redisLock,
                              PointsAwardLogger pointsAwardLogger, PlayerPackService playerPackService) {
        this.redissonClient = redissonClient;
        this.clusterSystem = clusterSystem;
        this.leaderboardService = leaderboardService;
        this.marsCurator = marsCurator;
        this.redisLock = redisLock;
        this.pointsAwardLogger = pointsAwardLogger;
        this.playerPackService = playerPackService;
    }

    /**
     * 初始化
     */
    public void init() {
        initDate = LocalDate.now();
        // 初始化充值数据记录map
        redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS,
                () -> rechargeMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RECHARGE));
        log.debug("初始化充值数据记录map完成");
    }

    /**
     * 跨天
     */
    public void daily() {
        //跨月检查
        checkMonth();
        if (marsCurator.isMaster()) {
            // 初始化充值数据记录map
            redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS,
                    () -> {
                        rechargeMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RECHARGE);
                        if (rechargeMap != null) {
                            rechargeMap.clear();
                        }
                    });
            log.debug("充值数据记录map清除完成");
        }
    }

    /**
     * 检查跨月
     */
    private void checkMonth() {
        LocalDate now = LocalDate.now();
        if (now.getMonthValue() != initDate.getMonthValue()) {
            // 初始化充值数据记录map
            redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS,
                    () -> clusterSystem.getAllOnlinePlayerId().forEach(playerId -> getLadderReceiveSet(playerId).clear()));
            initDate = now;
        }
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
        leaderboardService.login(player.getId());
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
     * @param type        {@link PointsAwardType}
     */
    @Override
    @RpcCallSetting(processorModKey = "#arg1")
    public void add(long playerId, int pointsAward, int type) {
        if (pointsAward <= 0) {
            return;
        }
        try {
            redisLock.lockAndRun(lockKey(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> addWithoutLock(playerId, pointsAward, type));
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
    public void addWithoutLock(long playerId, int pointsAward, int type) {
        if (pointsAward <= 0) {
            return;
        }
        RAtomicLong counter = redissonClient.getAtomicLong(atomicKey(playerId));
        try {
            long currentPoints = counter.get();
            updatePointsWithOverflowProtection(counter, currentPoints, pointsAward);
            //通知玩家同步分数
            noticeSyncPoints(playerId, counter.get());
        } catch (Exception e) {
            log.error("add 玩家积分更新失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
        }
        // 排行榜更新
        updateLeaderboards(playerId, pointsAward);
        //记录日志
        pointsAwardLogger.pointsChangeLog(playerId, pointsAward, type, true, counter.get());
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
    private void updateLeaderboards(long playerId, long pointsAward) {
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
    @Override
    @RpcCallSetting(processorModKey = "#arg1")
    public boolean deduct(long playerId, int pointsAward, int type) {
        if (pointsAward <= 0) {
            return false;
        }
        try {
            // 使用分布式锁，确保判断与扣减原子执行
            return redisLock.lockAndGet(lockKey(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> deductWithoutLock(playerId, pointsAward, type));
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
    public boolean deductWithoutLock(long playerId, int pointsAward, int type) {
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
                    //记录日志
                    pointsAwardLogger.pointsChangeLog(playerId, pointsAward, type, false, next);
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
    public <T> T deduct(long playerId, int pointsAward, Supplier<Boolean> supplier, Supplier<T> success, T defaultValue, int type) {
        return lockAndGet(playerId, () -> {
            boolean result = supplier.get();
            if (result) {
                boolean deducted = deductWithoutLock(playerId, pointsAward, type);
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
        syncPlayerPoint.setState(2);
        syncPlayerPoint.setRank(leaderboardService.getRank(PointsAwardConstant.Leaderboard.TYPE_MONTH, player));
        PFSession pfSession = clusterSystem.getSession(player);
        if (pfSession != null) {
            pfSession.send(syncPlayerPoint);
        }
    }

    /**
     * 玩家累计充值金额
     *
     * @param playerId 玩家id
     */
    public long getRecharge(long playerId) {
        return rechargeMap.getOrDefault(playerId, 0L);
    }

    /**
     * 玩家充值
     *
     * @param order 订单信息
     */
    public void recharge(Order order) {
        long playerId = order.getPlayerId();
        long price = order.getPrice().longValue();
        RLock lock = rechargeMap.getReadWriteLock(playerId).writeLock();
        if (lock.tryLock()) {
            long resultValue = getRecharge(playerId) + price;
            //增加玩家充值金额
            rechargeMap.fastPut(playerId, resultValue);
            lock.unlock();
        }
    }

    /**
     * 获取玩家已经领取的阶梯奖励列表
     */
    public RSet<Long> getLadderReceiveSet(long playerId) {
        return redissonClient.getSet(PointsAwardConstant.RedisKey.POINTS_AWARD_LADDER_REWARDS_RECEIVE + playerId);
    }

    /**
     * 获取配置的阶梯奖励
     */
    public List<PointsAwardLadderRewardsInfo> getLadderConfigInfoList(long playerId) {
        List<PointsAwardLadderRewardsInfo> resultList = new ArrayList<>();
        GlobalConfigCfg configCfg = GameDataManager.getGlobalConfigCfg(45);
        if (configCfg == null) {
            return resultList;
        }
        String configStr = configCfg.getValue();
        if (configStr == null) {
            return resultList;
        }
        String[] configs = configStr.split("\\|");
        RSet<Long> rewardReceiveSet = getLadderReceiveSet(playerId);
        RLock rLock = rewardReceiveSet.getReadWriteLock(playerId).readLock();
        try {
            if (rLock.tryLock()) {
                for (String config : configs) {
                    String[] configArray = config.split("_");
                    if (configArray.length < 3) {
                        continue;
                    }
                    PointsAwardLadderRewardsInfo pointsAwardLadderRewardsInfo = buildRewardInfoFromConfig(configArray, rewardReceiveSet);
                    resultList.add(pointsAwardLadderRewardsInfo);
                }
            }
        } finally {
            rLock.unlock();
        }
        return resultList;
    }

    private PointsAwardLadderRewardsInfo buildRewardInfoFromConfig(String[] configArray, RSet<Long> rewardReceiveSet) {
        int points = Integer.parseInt(configArray[0]);
        int rewardId = Integer.parseInt(configArray[1]);
        int count = Integer.parseInt(configArray[2]);
        PointsAwardLadderRewardsInfo pointsAwardLadderRewardsInfo = new PointsAwardLadderRewardsInfo();
        pointsAwardLadderRewardsInfo.setPoints(points);
        pointsAwardLadderRewardsInfo.setItemId(rewardId);
        pointsAwardLadderRewardsInfo.setItemNum(count);
        //已经领取过了
        if (rewardReceiveSet.contains(pointsAwardLadderRewardsInfo.getPoints())) {
            pointsAwardLadderRewardsInfo.setReceive(true);
        }
        return pointsAwardLadderRewardsInfo;
    }

    /**
     * 玩家领取积分阶梯奖励
     */
    public boolean receiveLader(long points, long playerId) {
        List<PointsAwardLadderRewardsInfo> configInfoList = getLadderConfigInfoList(playerId);
        Map<Long, PointsAwardLadderRewardsInfo> collect = configInfoList.stream()
                .collect(Collectors.toMap(PointsAwardLadderRewardsInfo::getPoints, Function.identity(), (oldValue, newValue) -> newValue));
        PointsAwardLadderRewardsInfo info = collect.get(points);
        if (info == null) {
            return false;
        }
        RSet<Long> rewardReceiveSet = getLadderReceiveSet(playerId);
        RLock rLock = rewardReceiveSet.getReadWriteLock(playerId).writeLock();
        try {
            if (rLock.tryLock()) {
                //已经领取过了
                if (rewardReceiveSet.contains(info.getPoints())) {
                    return false;
                }
                //奖励道具
                playerPackService.addItem(playerId, info.getItemId(), info.getItemNum(), "POINTS_AWARD_LADDER_REWARDS");
                rewardReceiveSet.add(info.getPoints());
                return true;
            }
        } finally {
            rLock.unlock();
        }
        return false;
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);
        if (gmOrders.length < 2) {
            result.code = Code.PARAM_ERROR;
            return result;
        }
        String code = gmOrders[0];
        if (!"addPoints".equalsIgnoreCase(code)) {
            result.code = Code.PARAM_ERROR;
            return result;
        }
        int points = Integer.parseInt(gmOrders[1]);
        if (points <= 0) {
            result.code = Code.PARAM_ERROR;
            return result;
        }
        add(playerController.playerId(), points, PointsAwardType.GM);
        return result;
    }
}
