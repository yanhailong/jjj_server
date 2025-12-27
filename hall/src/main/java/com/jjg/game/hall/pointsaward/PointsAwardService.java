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
import com.jjg.game.core.listener.ConfigExcelChangeListener;
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
import org.redisson.client.codec.LongCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 积分大奖积分服务
 */
@Service
public class PointsAwardService implements IPlayerLoginSuccess, GmListener, HallPointsAwardBridge, IRedDotService, ConfigExcelChangeListener {

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

    private Map<Long, PointsAwardLadderRewardsInfo> pointsAwardMap;
    private List<PointsAwardLadderRewardsInfo> sortPointsAwardList;

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
                () -> rechargeMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RECHARGE, LongCodec.INSTANCE));
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
        if (marsCurator.isMaster()) {
            //跨月检查
            clear();
            // 初始化充值数据记录map
            redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT, PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS,
                    () -> {
                        rechargeMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RECHARGE, LongCodec.INSTANCE);
                        if (rechargeMap != null) {
                            rechargeMap.clear();
                        }
                        RKeys keys = redissonClient.getKeys();
                        long deleted = keys.deleteByPattern(PointsAwardConstant.RedisKey.POINTS_AWARD_LADDER_REWARDS_RECEIVE + "*");
                        log.info("阶段奖励领取记录 删除数量: {}", deleted);
                    });
            log.debug("充值数据记录map清除完成");
        }
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

        //排行榜的积分
        long currentPoints = counter.get();
        try {
            currentPoints = updatePointsWithOverflowProtection(counter, currentPoints, pointsAward);
        } catch (Exception e) {
            log.error("add 玩家积分更新失败!playerId = [{}],pointsAward = [{}]", playerId, pointsAward, e);
        }
        // 排行榜更新
        updateLeaderboards(playerId, pointsAward);
        //记录日志
        pointsAwardLogger.pointsChangeLog(playerId, pointsAward, type, true, currentPoints);
        //添加时间段积分
        addTimePoints(playerId, pointsAward);
        //通知红点
        redDotManager.updateRedDotByInitialize(getModule(), List.of(getSubmodule(), PointsAwardConstant.RedDotSubModule.TURNRABLE), playerId);
        //通知玩家同步分数
        noticeSyncPoints(playerId, currentPoints);
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
    private long updatePointsWithOverflowProtection(RAtomicLong counter, long currentPoints, int pointsToAdd) {
        // 检查是否会溢出
        if (Long.MAX_VALUE - currentPoints < (long) pointsToAdd) {
            setToMaxValue(counter);
            return counter.get();
        } else {
            return counter.addAndGet(pointsToAdd);
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
            // 更新日榜
            leaderboardService.upsert(PointsAwardConstant.Leaderboard.DAY, playerId, pointsAward);
            // 更新周榜
            leaderboardService.upsert(PointsAwardConstant.Leaderboard.WEEK, playerId, pointsAward);
            // 同步更新月榜(用于每月结算快照)
            leaderboardService.upsert(PointsAwardConstant.Leaderboard.TYPE_MONTH, playerId, pointsAward);
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
                    updateLeaderboards(playerId, -pointsAward);
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
        rechargeMap.addAndGet(order.getPlayerId(), RedisUtils.toLong(order.getPrice()));
    }

    /**
     * 获取玩家已经领取的阶梯奖励列表
     */
    public RSet<Long> getLadderReceiveSet(long playerId) {
        return redissonClient.getSet(PointsAwardConstant.RedisKey.POINTS_AWARD_LADDER_REWARDS_RECEIVE + playerId);
    }

    /**
     * 重置时间段积分
     */
    public void resetTimePoints() {
        RMap<Long, TimePoints> playerTimePointsMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_TIME_DATA_POINTS);
        //先全部读取，然后删除
        Map<Long, TimePoints> timePointsMap = playerTimePointsMap.readAllMap();
        playerTimePointsMap.delete();

        if (this.pointsAwardMap == null || this.pointsAwardMap.isEmpty()) {
            log.debug("积分大奖保底奖励为空，故阶段积分重置时无奖励");
        } else {
            timePointsMap.forEach((playerId, timePoints) -> {
                this.pointsAwardMap.forEach((pointsAward, pointsAwardInfo) -> {
                    if (pointsAwardInfo.getPoints() <= timePoints.getPoints()) {
                        receiveLader(pointsAwardInfo.getPoints(), playerId, true);
                    }
                });
                getLadderReceiveSet(playerId).delete();
            });
        }

        log.debug("重置时间段积分 map.size = {}", timePointsMap.size());
    }

    /**
     * 玩家领取积分阶梯奖励
     */
    public int receiveLader(long points, long playerId, boolean autoRecive) {
        if (this.pointsAwardMap == null || this.pointsAwardMap.isEmpty()) {
            log.debug("积分大奖保底奖励为空，玩家领取奖励失败 playerId = {},points = {},autoRecive = {}", playerId, points, autoRecive);
            return Code.SAMPLE_ERROR;
        }

        PointsAwardLadderRewardsInfo info = this.pointsAwardMap.get(points);
        if (info == null) {
            log.debug("积分大奖奖励中无该阶段配置 playerId = {},points = {},autoRecive = {}", playerId, points, autoRecive);
            return Code.SAMPLE_ERROR;
        }
        //获取积分
        long timePoints = getTimePoints(playerId);
        if (timePoints < info.getPoints()) {
            return Code.POINT_AWARD_POINT_NOT_ENOUGH;
        }
        int code = Code.FAIL;
        RSet<Long> rewardReceiveSet = getLadderReceiveSet(playerId);
        RLock rLock = rewardReceiveSet.getReadWriteLock(playerId).writeLock();
        CommonResult<ItemOperationResult> addResult = null;
        try {
            if (rLock.tryLock()) {
                //已经领取过了
                if (rewardReceiveSet.contains(info.getPoints())) {
                    code = Code.REPEAT_OP;
                } else {
                    if (autoRecive) {
                        Item item = new Item(info.getItemId(), info.getItemNum());
                        mailService.addCfgMail(playerId, GameConstant.Mail.ID_POINTS_AWARD, List.of(item));
                    } else {
                        //奖励道具
                        addResult = playerPackService.addItem(playerId, info.getItemId(), info.getItemNum(), AddType.POINTS_AWARD_LADDER_REWARDS);
                        if (!addResult.success()) {
                            log.warn("玩家领取积分阶梯奖励失败 playerId = {},points = {},code = {}", playerId, points, addResult.code);
                            code = addResult.code;
                        } else {
                            rewardReceiveSet.add(info.getPoints());
                            code = Code.SUCCESS;
                        }
                    }
                }

            }
        } finally {
            rLock.unlock();
        }

        if (code == Code.SUCCESS) {
            pointsAwardLogger.ladderReward(playerId, points, addResult.data.getChangeGoldNum(), addResult.data.getGoldNum(), autoRecive);
        }
        return code;
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
        if (this.sortPointsAwardList == null || this.sortPointsAwardList.isEmpty()) {
            return List.of();
        }
        //获取玩家积分
        long points = getTimePoints(playerId);
        //获取领取列表
        RSet<Long> rewardReceiveSet = getLadderReceiveSet(playerId);
        RedDotDetails redDotDetails = new RedDotDetails();
        redDotDetails.setRedDotModule(getModule());
        redDotDetails.setRedDotSubmodule(getSubmodule());
        redDotDetails.setRedDotType(RedDotDetails.RedDotType.COMMON);
        for (PointsAwardLadderRewardsInfo rewardsInfo : this.sortPointsAwardList) {
            //积分比配置大并且未领取的代表有后点
            if (points >= rewardsInfo.getPoints() && !rewardReceiveSet.contains(rewardsInfo.getPoints())) {
                redDotDetails.setCount(1);
                break;
            }
        }
        return List.of(redDotDetails);
    }

    @Override
    public void initSampleCallbackCollector() {
        // global表监听
        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::initConfig)
                .addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::initConfig);
    }

    private void initConfig() {
        GlobalConfigCfg configCfg = GameDataManager.getGlobalConfigCfg(PointsAwardConstant.GlobalConfig.ID_POINTS_AWARD);
        if (configCfg == null) {
            log.debug("global表未找到积分大奖保底奖励配置 id = {}", PointsAwardConstant.GlobalConfig.ID_POINTS_AWARD);
            return;
        }
        String configStr = configCfg.getValue();
        if (configStr == null) {
            log.debug("global表未找到积分大奖保底奖励配置1 id = {}", PointsAwardConstant.GlobalConfig.ID_POINTS_AWARD);
            return;
        }

        Map<Long, PointsAwardLadderRewardsInfo> tmpPointsAwardMap = new HashMap<>();

        String[] configs = configStr.split("\\|");
        for (String str : configs) {
            String[] configArray = str.split("_");
            if (configArray.length < 3) {
                continue;
            }

            long points = Long.parseLong(configArray[0]);
            int itemId = Integer.parseInt(configArray[1]);
            int count = Integer.parseInt(configArray[2]);

            PointsAwardLadderRewardsInfo pointsAwardLadderRewardsInfo = new PointsAwardLadderRewardsInfo();
            pointsAwardLadderRewardsInfo.setPoints(points);
            pointsAwardLadderRewardsInfo.setItemId(itemId);
            pointsAwardLadderRewardsInfo.setItemNum(count);
            tmpPointsAwardMap.put(points, pointsAwardLadderRewardsInfo);
        }
        this.sortPointsAwardList = tmpPointsAwardMap.values()
                .stream()
                .sorted(Comparator.comparingLong(PointsAwardLadderRewardsInfo::getPoints))
                .toList();

        this.pointsAwardMap = tmpPointsAwardMap;
        log.debug("加载 积分大奖配置结束 id = {}", PointsAwardConstant.GlobalConfig.ID_POINTS_AWARD);
    }

    public List<PointsAwardLadderRewardsInfo> getPointsAwardLadderRewardsInfoList(long playerId) {
        if (this.pointsAwardMap == null || this.pointsAwardMap.isEmpty()) {
            return Collections.emptyList();
        }

        RSet<Long> rewardReceiveSet = getLadderReceiveSet(playerId);
        Set<Long> rewardReceiveIds = rewardReceiveSet.readAll();
        if (rewardReceiveIds == null || rewardReceiveIds.isEmpty()) {
            return this.sortPointsAwardList;
        } else {
            List<PointsAwardLadderRewardsInfo> list = new ArrayList<>();
            this.sortPointsAwardList.forEach(info -> {
                PointsAwardLadderRewardsInfo newInfo = new PointsAwardLadderRewardsInfo();
                BeanUtils.copyProperties(info, newInfo);
                if (rewardReceiveIds.contains(info.getPoints())) {
                    newInfo.setReceive(true);
                }
                list.add(newInfo);
            });
            return list;
        }
    }

    @Override
    public int getSubmodule() {
        return PointsAwardConstant.RedDotSubModule.BONUS;
    }
}
