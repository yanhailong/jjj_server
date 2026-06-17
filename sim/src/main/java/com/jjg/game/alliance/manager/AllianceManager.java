package com.jjg.game.alliance.manager;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceIdDao;
import com.jjg.game.alliance.service.AllianceBattleService;
import com.jjg.game.alliance.service.AllianceRankService;
import com.jjg.game.alliance.service.AllianceService;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 联盟模块管理器: 生命周期 + 集群单点调度。
 * <p>
 * 定时任务只在集群 leader 节点运行 (ZK 选举回调 {@link #isLeader()}/{@link #notLeader()}):
 * <ul>
 *   <li>对决状态机 tick (30s): 内部全部为旧状态条件更新, leader 切换双跑无副作用;</li>
 *   <li>榜单结算检查 (每小时): 周一结算上周贡献度周榜 / 每月 1 号结算上月赛季榜,
 *       Redis SETNX 防重复进入 + 结算自身按"榜单 key 存在性"幂等, 双保险。</li>
 * </ul>
 * 玩家登录时刷新成员活跃时间 (对决报名"活跃人数"模式的统计依据)。
 *
 * @author 11
 * @date 2026/6/11
 */
@Component
public class AllianceManager implements IGameClusterLeaderListener, IPlayerLoginSuccess {
    private static final Logger log = LoggerFactory.getLogger(AllianceManager.class);

    //结算标记 TTL: 覆盖结算周期即可
    private static final long WEEK_MARK_TTL_DAYS = 8;
    private static final long SEASON_MARK_TTL_DAYS = 40;

    @Autowired
    private AllianceService allianceService;
    @Autowired
    private AllianceBattleService battleService;
    @Autowired
    private AllianceRankService rankService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AllianceIdDao allianceIdDao;

    //leader 任务句柄
    private volatile Timeout battleTickTimeout;
    private volatile Timeout settleCheckTimeout;
    private volatile boolean running = false;

    public void init() {
        allianceIdDao.init();
        running = true;
        log.info("联盟模块初始化完成");
    }

    public void shutdown() {
        running = false;
        notLeader();
        log.info("联盟模块已关闭");
    }

    // ----------------------- 集群 leader 调度 -----------------------

    @Override
    public void isLeader() {
        if (!running) {
            return;
        }
        //对决状态机推进
        battleTickTimeout = WheelTimerUtil.scheduleAtFixedRate(this::safeBattleTick, 10, 30, TimeUnit.SECONDS);
        //榜单结算检查
        settleCheckTimeout = WheelTimerUtil.scheduleAtFixedRate(this::safeSettleCheck, 60, 3600, TimeUnit.SECONDS);
        log.info("成为集群leader, 启动联盟调度任务");
    }

    @Override
    public void notLeader() {
        if (battleTickTimeout != null) {
            battleTickTimeout.cancel();
            battleTickTimeout = null;
        }
        if (settleCheckTimeout != null) {
            settleCheckTimeout.cancel();
            settleCheckTimeout = null;
        }
    }

    private void safeBattleTick() {
        try {
            battleService.tick();
        } catch (Exception e) {
            log.error("联盟对决tick异常", e);
        }
    }

    /**
     * 结算检查: SETNX 标记防止 leader 在位期间每小时重复扫描;
     * 结算逻辑本身按"榜单 key 是否存在"幂等, 标记丢失也不会重复发奖。
     */
    private void safeSettleCheck() {
        try {
            LocalDate today = LocalDate.now();
            //周一: 结算上周贡献度周榜
            if (today.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
                String mark = AllianceConst.RedisKey.RANK_SETTLE_LOCK + ":week:" + today;
                if (Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                        .setIfAbsent(mark, "1", WEEK_MARK_TTL_DAYS, TimeUnit.DAYS))) {
                    rankService.settleWeeklyContrib();
                }
            }
            //每月 1 号: 结算上月赛季榜
            if (today.getDayOfMonth() == 1) {
                String mark = AllianceConst.RedisKey.RANK_SETTLE_LOCK + ":season:" + today;
                if (Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                        .setIfAbsent(mark, "1", SEASON_MARK_TTL_DAYS, TimeUnit.DAYS))) {
                    rankService.settleSeason();
                }
            }
        } catch (Exception e) {
            log.error("联盟榜单结算检查异常", e);
        }
    }

    // ----------------------- 登录联动 -----------------------

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, Account account, boolean firstLogin) {
        try {
            allianceService.onPlayerLogin(playerController.playerId());
        } catch (Exception e) {
            log.error("登录刷新联盟活跃时间失败 playerId={}", playerController.playerId(), e);
        }
    }
}
