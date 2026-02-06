package com.jjg.game.hall.pointsaward.signin;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.PointsAwardType;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.pointsaward.PointsAwardLogger;
import com.jjg.game.hall.pointsaward.PointsAwardService;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardSignInConfig;
import com.jjg.game.sampledata.bean.PointsAwardSigninCfg;
import jakarta.annotation.PreDestroy;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * 签到服务
 */
@Service
public class PointsAwardSignInService implements IRedDotService, IPlayerLoginSuccess {

    private static final Logger log = LoggerFactory.getLogger(PointsAwardSignInService.class);
    private final PointsAwardService pointsAwardService;
    private final ClusterSystem clusterSystem;
    private final PlayerPackService playerPackService;
    private final RedissonClient redissonClient;
    private final RedisLock redisLock;
    private final RedDotManager redDotManager;
    private final PointsAwardLogger pointsAwardLogger;

    /**
     * 签到管理器
     */
    private PointsAwardSignInManager manager;

    /**
     * 虚拟线程池
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PointsAwardSignInService(PointsAwardService pointsAwardService,
                                    PlayerPackService playerPackService,
                                    RedissonClient redissonClient,
                                    RedisLock redisLock,
                                    ClusterSystem clusterSystem,
                                    RedDotManager redDotManager,
                                    PointsAwardLogger pointsAwardLogger) {
        this.pointsAwardService = pointsAwardService;
        this.playerPackService = playerPackService;
        this.redissonClient = redissonClient;
        this.redisLock = redisLock;
        this.clusterSystem = clusterSystem;
        this.redDotManager = redDotManager;
        this.pointsAwardLogger = pointsAwardLogger;
    }

    public void init(PointsAwardSignInManager manager) {
        this.manager = manager;
    }

    /**
     * 签到锁
     *
     * @param playerId 玩家id
     */
    public String signLock(long playerId) {
        return PointsAwardConstant.RedisLockKey.POINTS_AWARD_SIGN_IN_LOCK + playerId;
    }

    /**
     * 清除签到数据 跨月了
     */
    public void clear() {
        redisLock.tryLockAndRun(PointsAwardConstant.RedisLockKey.POINTS_AWARD_SIGN_IN_CLEAR_LOCK, () -> {
            RKeys keys = redissonClient.getKeys();
            long deleted = keys.deleteByPattern(PointsAwardConstant.RedisKey.POINTS_AWARD_SIGN_IN_SET + "*");
            log.info("清除签到数据 删除数量: {}", deleted);
            deleted = keys.deleteByPattern(PointsAwardConstant.RedisKey.POINTS_AWARD_SIGN_IN_UNLOCK_SET + "*");
            log.info("清除签到解锁数据 删除数量: {}", deleted);
            getDateMap().clear();
            log.info("清除签到时间数据map!");
        });
    }

    /**
     * 跨天
     */
    public void daily() {
        Set<Long> onlinePlayerId = clusterSystem.getAllOnlinePlayerId();
        if (!onlinePlayerId.isEmpty()) {
            //清除签到数据 怕人多用虚拟线程多线程执行
            onlinePlayerId.forEach(playerId -> executor.submit(() -> {
                try {
                    //解锁新签到
                    redisLock.lockAndRun(signLock(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> unlock(playerId));
                } catch (Exception e) {
                    log.error("玩家[{}]跨月解锁签到数据错误!", playerId, e);
                }
            }));
        }
    }

    /**
     * 判断是否需要清空签到数据
     *
     * @param lastSignDate 上次签到日期
     * @param now          当前日期
     * @return true 表示需要清空数据
     */
    public boolean needReset(LocalDate lastSignDate, LocalDate now) {
        // 如果从未签到，直接清空（或初始化）
        if (lastSignDate == null) {
            return true;
        }
        // 比较年月
        YearMonth lastYM = YearMonth.from(lastSignDate);
        YearMonth currentYM = YearMonth.from(now);
        return !lastYM.equals(currentYM);
    }

    /**
     * 检测签到数据
     */
    public void check(long playerId) {
        RMap<Long, Long> dateMap = getDateMap();
        if (dateMap == null) {
            return;
        }
        long lastSignTime = dateMap.getOrDefault(playerId, 0L);
        LocalDate now = LocalDate.now();
        //签到过才计算
        if (lastSignTime > 0) {
            LocalDate dateTime = LocalDate.ofInstant(Instant.ofEpochMilli(lastSignTime), ZoneId.systemDefault());
            //检测是否需要清空签到数据
            if (needReset(dateTime, now)) {
                getSignSet(playerId).clear();
                getUnlockSet(playerId).clear();
                dateMap.remove(playerId);
            }
        }
    }

    /**
     * 获取签到配置列表
     *
     * @return 签到配置列表
     */
    public List<PointsAwardSignInConfig> getConfigList(long playerId) {
        List<PointsAwardSigninCfg> signInCfgList = manager.getSignInCfgList();
        if (signInCfgList == null || signInCfgList.isEmpty()) {
            return null;
        }
        RSet<Integer> unlockSet = getUnlockSet(playerId);
        RSet<Integer> signSet = getSignSet(playerId);
        return signInCfgList.stream().map(cfg -> {
            PointsAwardSignInConfig config = new PointsAwardSignInConfig();
            config.setDayOfMonth(cfg.getId());
            config.setIntegralNum(cfg.getIntegralNum());
            config.setItemList(ItemUtils.buildItemInfos(cfg.getGetItem()));
            //已领取
            if (signSet.contains(cfg.getId())) {
                config.setState(2);
            }
            //已解锁 未领取
            else if (unlockSet.contains(cfg.getId())) {
                config.setState(1);
            } else {
                config.setState(0);
            }
            return config;
        }).toList();
    }

    /**
     * 获取签到次数统计的映射表。
     *
     * @return 一个包含玩家ID和签到次数的映射表。如果映射表不存在，则返回空映射。
     */
    public RSet<Integer> getSignSet(long playerId) {
        return redissonClient.getSet(PointsAwardConstant.RedisKey.POINTS_AWARD_SIGN_IN_SET + playerId);
    }

    /**
     * 获取签到奖励解锁次数统计的映射表。
     */
    public RSet<Integer> getUnlockSet(long playerId) {
        return redissonClient.getSet(PointsAwardConstant.RedisKey.POINTS_AWARD_SIGN_IN_UNLOCK_SET + playerId);
    }

    /**
     * 获取玩家的最后签到日期的映射表。
     *
     * @return 一个包含玩家ID与最后签到时间戳（毫秒值）之间关系的映射表。
     */
    public RMap<Long, Long> getDateMap() {
        return redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_SIGN_IN_DATE);
    }

    /**
     * 获取玩家最后一次签到时间
     *
     * @param playerId 玩家id
     */
    public long getLastSignInTime(long playerId) {
        RMap<Long, Long> pointsMap = getDateMap();
        if (pointsMap != null) {
            return pointsMap.getOrDefault(playerId, 0L);
        }
        return 0L;
    }

    /**
     * 玩家请求签到
     *
     * @param player 玩家
     * @return true 签到成功
     */
    public void signIn(Player player, int dayOfMonth) {
        PointsAwardSigninCfg signInCfg = manager.getSignInCfg(dayOfMonth);
        if (signInCfg == null) {
            return;
        }
        //检测条件
        Supplier<Boolean> condition = () -> {
            RSet<Integer> unlockSet = getUnlockSet(player.getId());
            if (unlockSet == null) {
                return false;
            }
            //未解锁不能领取奖励
            if (!unlockSet.contains(dayOfMonth)) {
                return false;
            }
            RSet<Integer> signSet = getSignSet(player.getId());
            //重复领取
            return !signSet.contains(dayOfMonth);
        };
        if (!condition.get()) {
            return;
        }
        boolean success = redisLock.tryLockAndGet(signLock(player.getId()), () -> {
            if (!condition.get()) {
                return false;
            }
            //记录签到奖励领取数据
            getSignSet(player.getId()).add(dayOfMonth);
            return true;
        }, false);
        if (success) {
            if (signInCfg.getIntegralNum() > 0) {
                pointsAwardService.add(player.getId(), signInCfg.getIntegralNum(), PointsAwardType.SIGN);
            }
            //发送道具奖励
            if (signInCfg.getGetItem() != null && !signInCfg.getGetItem().isEmpty()) {
                playerPackService.addItems(player.getId(), ItemUtils.buildItems(signInCfg.getGetItem()), AddType.POINTS_AWARD_SIGN_REWARDS);
            }
            //记录日志
            pointsAwardLogger.signInLog(player, getSignSet(player.getId()).size(), signInCfg.getIntegralNum(), pointsAwardService.getPoints(player.getId()));
            //更新红点
            redDotManager.updateRedDotByInitialize(getModule(), getSubmodule(), player.getId());
        }
    }

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.POINTS_AWARD;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        int currCount = getSignSet(playerId).size();
        int unlockCount = getUnlockSet(playerId).size();
        RedDotDetails details = new RedDotDetails();
        details.setRedDotModule(getModule());
        details.setCount(currCount < unlockCount ? 1 : 0);
        details.setRedDotSubmodule(getSubmodule());
        details.setRedDotType(RedDotDetails.RedDotType.COMMON);
        return List.of(details);
    }

    @Override
    public int getSubmodule() {
        return PointsAwardConstant.RedDotSubModule.SIGN_IN;
    }

    /**
     * 玩家解锁今日签到
     *
     * @param playerId 玩家id
     */
    public void unlock(long playerId) {
        RSet<Integer> unlockSet = getUnlockSet(playerId);
        //当前需要解锁的天数
        int day = unlockSet.size() + 1;
        int signInMaxCount = manager.getSignInMaxCount();
        //签到满了已经
        if (day >= signInMaxCount) {
            return;
        }
        PointsAwardSigninCfg todayConfig = manager.getSignInCfg(day);
        if (todayConfig == null) {
            return;
        }
        //签到时间map
        RMap<Long, Long> dateMap = getDateMap();
        Long signTime = dateMap.get(playerId);
        //验证今天是否签到过
        if (signTime != null && signTime > 0) {
            LocalDate signDate = LocalDate.ofInstant(Instant.ofEpochMilli(signTime), ZoneId.systemDefault());
            //今天签到过了
            if (signDate.isEqual(LocalDate.now())) {
                return;
            }
        }
        log.info("玩家[{}]解锁[{}]签到 signTime = [{}]!", playerId, day, signTime);
        //解锁今天的签到配置
        unlockSet.add(todayConfig.getId());
        //记录时间 用来清除记录
        dateMap.fastPut(playerId, System.currentTimeMillis());
        //更新红点
        redDotManager.updateRedDotByInitialize(getModule(), 0, playerId);
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
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, Account account, boolean firstLogin) {
        long playerId = playerController.playerId();
        redisLock.lockAndRun(signLock(playerId), PointsAwardConstant.WaitTime.LOCK_LEASE_MILLIS, () -> {
            //检测玩家数据是否需要重置
            check(playerId);
            //解锁玩家签到数据
            unlock(playerId);
        });
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
    }
}
