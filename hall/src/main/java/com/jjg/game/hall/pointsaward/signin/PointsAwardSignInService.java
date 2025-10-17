package com.jjg.game.hall.pointsaward.signin;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.pointsaward.PointsAwardService;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardSignInConfig;
import com.jjg.game.sampledata.bean.PointsAwardSigninCfg;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * 签到服务
 */
@Service
public class PointsAwardSignInService implements IRedDotService {

    private final PointsAwardService pointsAwardService;
    private final ClusterSystem clusterSystem;
    private final PlayerPackService playerPackService;
    private final RedissonClient redissonClient;
    private final RedisLock redisLock;
    private final RedDotManager redDotManager;

    /**
     * 签到管理器
     */
    private PointsAwardSignInManager manager;

    public PointsAwardSignInService(PointsAwardService pointsAwardService,
                                    PlayerPackService playerPackService,
                                    RedissonClient redissonClient,
                                    RedisLock redisLock,
                                    ClusterSystem clusterSystem,
                                    RedDotManager redDotManager) {
        this.pointsAwardService = pointsAwardService;
        this.playerPackService = playerPackService;
        this.redissonClient = redissonClient;
        this.redisLock = redisLock;
        this.clusterSystem = clusterSystem;
        this.redDotManager = redDotManager;
    }

    public void init(PointsAwardSignInManager manager) {
        this.manager = manager;
    }

    /**
     * 最后一次签到时间key
     *
     * @param playerId 玩家id
     */
    public String signDateKey(long playerId) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_SING_IN_DATE + playerId;
    }

    /**
     * 签到锁
     *
     * @param playerId 玩家id
     */
    public String signLock(long playerId) {
        return PointsAwardConstant.RedisLockKey.POINTS_AWARD_SING_IN_LOCK + playerId;
    }

    /**
     * 清除签到数据 跨月了
     */
    public void clear() {
        Set<Long> onlinePlayerId = clusterSystem.getAllOnlinePlayerId();
        if (!onlinePlayerId.isEmpty()) {
            onlinePlayerId.forEach(this::check);
        }
    }

    /**
     * 跨天
     */
    public void daily() {
        //清除玩家今天的签到数据
        Set<Long> onlinePlayerId = clusterSystem.getAllOnlinePlayerId();
        if (!onlinePlayerId.isEmpty()) {
            onlinePlayerId.forEach(playerId -> {
                getDateMap().remove(playerId);
                //更新红点
                redDotManager.updateRedDot(this, 0, playerId);
            });
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
                RMap<Long, Integer> countMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_SING_IN_COUNT);
                if (countMap != null) {
                    countMap.remove(playerId);
                }
                dateMap.remove(playerId);
                //更新红点
                redDotManager.updateRedDot(this, 0, playerId);
            }
        }
    }

    /**
     * 获取签到配置列表
     *
     * @return 签到配置列表
     */
    public List<PointsAwardSignInConfig> getConfigList() {
        List<PointsAwardSigninCfg> signInCfgList = manager.getSignInCfgList();
        if (signInCfgList == null || signInCfgList.isEmpty()) {
            return null;
        }
        return signInCfgList.stream().map(cfg -> {
            PointsAwardSignInConfig config = new PointsAwardSignInConfig();
            config.setDayOfMonth(cfg.getId());
            config.setIntegralNum(cfg.getIntegralNum());
            config.setItemList(ItemUtils.buildItemInfos(cfg.getGetItem()));
            return config;
        }).toList();
    }

    /**
     * 获取签到次数统计的映射表。
     *
     * @return 一个包含玩家ID和签到次数的映射表。如果映射表不存在，则返回空映射。
     */
    public RMap<Long, Integer> getCountMap() {
        return redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_SING_IN_COUNT);
    }

    /**
     * 获取玩家签到天数
     *
     * @param playerId 玩家id
     * @return 0表示没有签到过
     */
    public int getSignCount(long playerId) {
        RMap<Long, Integer> countMap = getCountMap();
        if (countMap != null) {
            return countMap.getOrDefault(playerId, 0);
        }
        return 0;
    }

    /**
     * 获取玩家的最后签到日期的映射表。
     *
     * @return 一个包含玩家ID与最后签到时间戳（毫秒值）之间关系的映射表。
     */
    public RMap<Long, Long> getDateMap() {
        return redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_SING_IN_DATE);
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
     * @param playerId 玩家id
     * @return true 签到成功
     */
    public boolean signIn(long playerId) {
        PointsAwardSigninCfg signInCfg = manager.getSignInCfg(LocalDate.now().getDayOfMonth());
        if (signInCfg == null) {
            return false;
        }
        boolean success = redisLock.tryLockAndGet(signLock(playerId), () -> {
            //签到次数
            int signCount = getSignCount(playerId);
            //最后一次签到时间
            long lastSignInTime = getLastSignInTime(playerId);
            //今天签到过了
            if (lastSignInTime > 0) {
                LocalDate dateTime = LocalDate.ofInstant(Instant.ofEpochMilli(lastSignInTime), ZoneId.systemDefault());
                if (dateTime.isEqual(LocalDate.now())) {
                    return false;
                }
            }
            //签到成功
            if (signCount < manager.getSignInMaxCount()) {
                getCountMap().put(playerId, signCount + 1);
                getDateMap().put(playerId, System.currentTimeMillis());
                return true;
            }
            return false;
        }, false);
        if (success) {
            if (signInCfg.getIntegralNum() > 0) {
                pointsAwardService.add(playerId, signInCfg.getIntegralNum());
            }
            //发送道具奖励
            if (signInCfg.getGetItem() != null && !signInCfg.getGetItem().isEmpty()) {
                playerPackService.addItems(playerId, ItemUtils.buildItems(signInCfg.getGetItem()), "积分大奖签到奖励");
            }
        }
        //更新红点
        redDotManager.updateRedDot(this, 0, playerId);
        return success;
    }

    /**
     * 检测当天能否签到
     */
    public boolean checkCanSign(long playerId) {
        Long time = getDateMap().get(playerId);
        if (time == null) {
            return true;
        }
        LocalDate now = LocalDate.now();
        LocalDate dateTime = LocalDate.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        return dateTime.isEqual(now);
    }

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.POINTS_AWARD;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        RedDotDetails details = new RedDotDetails();
        details.setRedDotModule(getModule());
        details.setRedDotSubmodule(PointsAwardConstant.RedDotSubModule.SIGN_IN);
        details.setRedDotType(RedDotDetails.RedDotType.COMMON);
        return List.of(details);
    }
}
