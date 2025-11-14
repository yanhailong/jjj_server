package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.rpc.RpcCallSetting;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.*;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.rpc.HallPointsAwardBridge;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.RedisUtils;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 积分大奖积分服务
 */
@Service
public class PointsAwardService implements IPlayerLoginSuccess, GmListener, HallPointsAwardBridge, IRedDotService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedissonClient redissonClient;
    private final PointsAwardLeaderboardService leaderboardService;
    private final RedisLock redisLock;
    private final ClusterSystem clusterSystem;
    private final MarsCurator marsCurator;
    private final PointsAwardLogger pointsAwardLogger;
    private final PlayerPackService playerPackService;
    private final RedDotManager redDotManager;
    private final MailService mailService;

    /**
     * 玩家累计充值金额
     */
    private RMap<Long, Long> rechargeMap;

    public PointsAwardService(RedissonClient redissonClient,
                              @Lazy ClusterSystem clusterSystem,
                              PointsAwardLeaderboardService leaderboardService,
                              MarsCurator marsCurator,
                              RedisLock redisLock,
                              PointsAwardLogger pointsAwardLogger, PlayerPackService playerPackService, RedDotManager redDotManager, MailService mailService) {
        this.redissonClient = redissonClient;
        this.clusterSystem = clusterSystem;
        this.leaderboardService = leaderboardService;
        this.marsCurator = marsCurator;
        this.redisLock = redisLock;
        this.pointsAwardLogger = pointsAwardLogger;
        this.playerPackService = playerPackService;
        this.redDotManager = redDotManager;
        this.mailService = mailService;
    }

    /**
     * 初始化
     */
    public void init() {
        // 初始化充值数据记录map
        redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS,
                () -> rechargeMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RECHARGE));
        //查看是否需要清除旧数据
        clear();
        RBucket<Long> bucket = redissonClient.getBucket(PointsAwardConstant.RedisKey.POINTS_AWARD_TIME);
        if (bucket.get() == null) {
            bucket.set(System.currentTimeMillis());
        }
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
        clear();
    }

    /**
     * 检测玩家数据是否需要清除
     */
    public void clear() {
        RBucket<Long> bucket = redissonClient.getBucket(PointsAwardConstant.RedisKey.POINTS_AWARD_TIME);
        Runnable command = () -> {
            // 初始化充值数据记录map
            redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS,
                    () -> {
                        RKeys keys = redissonClient.getKeys();
                        long deleted = keys.deleteByPattern(PointsAwardConstant.RedisKey.POINTS_AWARD_DATA_POINTS + "*");
                        log.info("玩家积分数据清除! 删除数量: {}", deleted);
                        deleted = keys.deleteByPattern(PointsAwardConstant.RedisKey.POINTS_AWARD_LADDER_REWARDS_RECEIVE + "*");
                        log.info("充值数据领取记录 删除数量: {}", deleted);
                    });
        };
        if (bucket.get() == null) {
            command.run();
        } else {
            long initDateMills = bucket.get();
            LocalDate initDate = LocalDate.ofInstant(Instant.ofEpochMilli(initDateMills), ZoneId.systemDefault());
            if (LocalDate.now().getMonthValue() != initDate.getMonthValue()) {
                bucket.set(System.currentTimeMillis());
                command.run();
            }
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
            //排行榜的积分
            long currentPoints = counter.get();
            updatePointsWithOverflowProtection(counter, currentPoints, pointsAward);
            //通知玩家同步分数
            noticeSyncPoints(playerId, counter.get());
        } catch (Exception e) {
            log.error("add 玩家积分更新失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
        }
        // 排行榜更新
        updateLeaderboards(playerId, counter.get());
        //记录日志
        pointsAwardLogger.pointsChangeLog(playerId, pointsAward, type, true, counter.get());
        //通知红点
        redDotManager.updateRedDotByInitialize(getModule(), List.of(getSubmodule(), PointsAwardConstant.RedDotSubModule.TURNRABLE), playerId);

        //添加时间段积分
        addTimePoints(playerId, pointsAward);
    }

    /**
     * 添加时间段积分
     *
     * @param playerId
     * @param pointsAward
     */
    private void addTimePoints(long playerId, int pointsAward) {
        String lockKey = PointsAwardConstant.RedisKey.POINTS_AWARD_TIME_DATA_POINTS_LOCK + playerId;

        RLock lock = null;

        try {
            lock = redissonClient.getLock(lockKey);

            boolean locked = lock.tryLock(PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new RuntimeException("获取时间段积分锁超时，playerId: " + playerId);
            }

            RMap<Long, TimePoints> playerTimePointsMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_TIME_DATA_POINTS);
            TimePoints timePoints = playerTimePointsMap.get(playerId);

            long currentTime = System.currentTimeMillis();

            if (timePoints == null) {
                timePoints = new TimePoints();
                timePoints.setPoints(pointsAward);
            } else {
                // 累加
                timePoints.setPoints(timePoints.getPoints() + pointsAward);
            }
            timePoints.setTime(currentTime);
            playerTimePointsMap.put(playerId, timePoints);
        } catch (Exception e) {
            log.error("更新玩家时间段积分异常 playerId = {},pointsAward = {}", playerId, pointsAward, e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
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
                    // 排行榜更新
                    updateLeaderboards(playerId, counter.get());
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
     * 获取时间段积分
     *
     * @param playerId
     * @return
     */
    public long getTimePoints(long playerId) {
        RMap<Long, TimePoints> playerTimePointsMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_TIME_DATA_POINTS);
        TimePoints timePoints = playerTimePointsMap.get(playerId);
        if (timePoints == null) {
            return 0;
        }
        return timePoints.getPoints();
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
    public BigDecimal getRecharge(long playerId) {
        Long recharge = rechargeMap.get(playerId);
        if (recharge == null) {
            return BigDecimal.ZERO;
        }
        return RedisUtils.fromLong(recharge);
    }

    /**
     * 玩家充值
     *
     * @param order 订单信息
     */
    public void recharge(Order order) {
        long playerId = order.getPlayerId();
        BigDecimal price = order.getPrice();
        RLock lock = rechargeMap.getReadWriteLock(playerId).writeLock();
        if (lock.tryLock()) {
            long resultValue = RedisUtils.toLong(getRecharge(playerId).add(price));
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
     * 重置时间段积分
     */
    public void resetTimePoints() {
        RMap<Long, TimePoints> playerTimePointsMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_TIME_DATA_POINTS);
        //先全部读取，然后删除
        Map<Long, TimePoints> timePointsMap = playerTimePointsMap.readAllMap();
        playerTimePointsMap.delete();

        timePointsMap.forEach((playerId, timePoints) -> {
            List<PointsAwardLadderRewardsInfo> configInfoList = getLadderConfigInfoList(playerId);
            for(PointsAwardLadderRewardsInfo info : configInfoList){
                if(info.getPoints() <= timePoints.getPoints()){
                    receiveLader(info.getPoints(), playerId, true);
                }
            }
            getLadderReceiveSet(playerId).delete();
        });

        log.debug("重置时间段积分 map.size = {}", timePointsMap.size());
    }

    /**
     * 玩家领取积分阶梯奖励
     */
    public boolean receiveLader(long points, long playerId, boolean autoRecive) {
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
                if (autoRecive) {
                    Item item = new Item(info.getItemId(), info.getItemNum());
                    mailService.addCfgMail(playerId, GameConstant.Mail.ID_POINTS_AWARD, List.of(item));
                } else {
                    //奖励道具
                    playerPackService.addItem(playerId, info.getItemId(), info.getItemNum(), AddType.POINTS_AWARD_SIGN_REWARDS);
                }
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

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.POINTS_AWARD;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        //获取玩家积分
        long points = getPoints(playerId);
        List<PointsAwardLadderRewardsInfo> configInfoList = getLadderConfigInfoList(playerId);
        //按积分升序
        configInfoList.sort(Comparator.comparingLong(PointsAwardLadderRewardsInfo::getPoints));
        //获取领取列表
        RSet<Long> rewardReceiveSet = getLadderReceiveSet(playerId);
        RedDotDetails redDotDetails = new RedDotDetails();
        redDotDetails.setRedDotModule(getModule());
        redDotDetails.setRedDotSubmodule(getSubmodule());
        redDotDetails.setRedDotType(RedDotDetails.RedDotType.COMMON);
        //已经领取的长度和配置长度一致直接返回
        if (configInfoList.size() == rewardReceiveSet.size()) {
            return List.of(redDotDetails);
        }
        for (PointsAwardLadderRewardsInfo rewardsInfo : configInfoList) {
            //积分比配置大并且未领取的代表有后点
            if (points >= rewardsInfo.getPoints() && !rewardReceiveSet.contains(rewardsInfo.getPoints())) {
                redDotDetails.setCount(1);
                break;
            }
        }
        return List.of(redDotDetails);
    }

    private boolean shouldRefreshTask(LocalDateTime createTime) {
        LocalDateTime now = LocalDateTime.now();
        // 不是同一天，肯定需要刷新
        if (!createTime.toLocalDate().isEqual(now.toLocalDate())) {
            return true;
        }
        // 同一天的情况下，判断是否跨越了12点
        int createHour = createTime.getHour();
        int nowHour = now.getHour();
        // 创建时间在0-11点（上半天），当前时间在12-23点（下半天）
        return createHour < TaskConstant.TimeConstants.NOON_HOUR && nowHour >= TaskConstant.TimeConstants.NOON_HOUR;
    }

    @Override
    public int getSubmodule() {
        return PointsAwardConstant.RedDotSubModule.BONUS;
    }
}
