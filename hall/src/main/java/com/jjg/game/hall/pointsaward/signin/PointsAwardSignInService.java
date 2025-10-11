package com.jjg.game.hall.pointsaward.signin;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.pointsaward.PlayerPointsAwardService;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.db.PointsAwardData;
import com.jjg.game.hall.pointsaward.pb.PointsAwardSignInConfig;
import com.jjg.game.sampledata.bean.PointsAwardSigninCfg;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 签到服务
 */
@Service
public class PointsAwardSignInService {

    private final PlayerPointsAwardService pointsAwardService;
    private final RedisLock redisLock;
    private final ClusterSystem clusterSystem;
    private final PlayerPackService playerPackService;

    /**
     * 签到管理器
     */
    private PointsAwardSignInManager manager;

    public PointsAwardSignInService(PlayerPointsAwardService pointsAwardService,
                                    PlayerPackService playerPackService,
                                    ClusterSystem clusterSystem,
                                    RedisLock redisLock) {
        this.pointsAwardService = pointsAwardService;
        this.playerPackService = playerPackService;
        this.clusterSystem = clusterSystem;
        this.redisLock = redisLock;
    }

    public void init(PointsAwardSignInManager manager) {
        this.manager = manager;
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
     *
     * @param playerId
     */
    public void check(long playerId) {
        pointsAwardService.updateWithLock(playerId, playerData -> {
            long lastSignTime = playerData.getLastSignInTime();
            LocalDate now = LocalDate.now();
            //签到过才计算
            if (lastSignTime > 0) {
                LocalDate dateTime = LocalDate.ofInstant(Instant.ofEpochMilli(lastSignTime), ZoneId.systemDefault());
                //检测是否需要清空签到数据
                if (needReset(dateTime, now)) {
                    playerData.setSignInCount(0);
                }
            }
        });
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
     * 获取玩家签到天数
     *
     * @param playerId 玩家id
     * @return 0表示没有签到过
     */
    public int getSignCount(long playerId) {
        PointsAwardData playerData = pointsAwardService.getPlayerData(playerId);
        if (playerData == null) {
            return 0;
        }
        return playerData.getSignInCount();
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
        boolean flag = redisLock.tryLockAndGet(PointsAwardConstant.RedisLockKey.POINTS_AWARD_SING_IN_LOCK, () -> {
            //获取当前签到天数
            int signCount = getSignCount(playerId);
            return signCount < manager.getSignInMaxCount();
        });
        if (flag) {
            Consumer<PointsAwardData> mutator = playerData -> {
                playerData.setSignInCount(playerData.getSignInCount() + 1);
                playerData.setLastSignInTime(System.currentTimeMillis());
            };
            if (signInCfg.getIntegralNum() > 0) {
                //增加积分
                pointsAwardService.add(playerId, signInCfg.getIntegralNum(), mutator);
            } else {
                pointsAwardService.updateWithLock(playerId, mutator);
            }
            //发送道具奖励
            if (signInCfg.getGetItem() != null && !signInCfg.getGetItem().isEmpty()) {
                playerPackService.addItems(playerId, ItemUtils.buildItems(signInCfg.getGetItem()), "积分大奖签到奖励");
            }
        }
        return flag;
    }

}
