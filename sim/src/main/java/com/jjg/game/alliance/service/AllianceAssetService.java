package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.pb.res.NotifyAlliance;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.service.RankService;
import com.jjg.game.sim.service.SimConfigCacheService;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

/**
 * 联盟资产统一授予门面: 声誉值(联盟) / 贡献值与贡献度(玩家)。
 * <p>
 * 捐献、任务、互助、对决、未来的联盟活动(PVP/PVE)产出全部从这里入账,
 * 保证"加声誉 -> 榜单同步 -> 等级重算 -> 升级广播"的链路只有一份。
 * 声誉/贡献写路径均为 Mongo $inc + Redis zset 累加, 多节点并发安全。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceAssetService {
    private static final Logger log = LoggerFactory.getLogger(AllianceAssetService.class);

    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private SimConfigCacheService configService;
    @Autowired
    private RankService rankService;
    @Autowired
    private SocialSender socialSender;

    // ----------------------- 声誉 (联盟侧) -----------------------

    /**
     * 给联盟累加声誉值: $inc 后按新值重算等级, 升级用条件更新保证全集群只广播一次;
     * 同步累加声誉总榜与赛季(自然月)榜。
     * <p>
     * 注: 声誉数值变化不主动失效联盟缓存 (展示允许 30s 内的滞后, 避免高频写把缓存打穿);
     * 等级变化才失效并广播。
     *
     * @return 累加后的联盟声誉值; 联盟不存在返回 -1
     */
    public long grantReputation(long allianceId, long delta) {
        if (allianceId <= 0 || delta <= 0) {
            return -1;
        }
        AllianceData updated = allianceDao.incReputation(allianceId, delta);
        if (updated == null) {
            //联盟已解散: 声誉作废 (需求: 解散后累计奖励与积分清除)
            return -1;
        }
        //榜单累加 (总榜 + 赛季榜)
        rankService.addPoints(AllianceConst.RedisKey.RANK_REPUTATION, allianceId, (int) delta);
        rankService.addPoints(seasonRankKey(), allianceId, (int) delta);

        //等级重算: 条件更新只升不降, 并发重算只有一个节点会成功并广播
        int newLevel = configService.allianceLevelOf(updated.getLevel(), updated.getReputation());
        if (newLevel > updated.getLevel() && allianceDao.tryUpgradeLevel(allianceId, newLevel)) {
            cacheService.publishInvalidate(allianceId);
            broadcastToAlliance(allianceId, AllianceConst.NotifyType.LEVEL_UP, String.valueOf(newLevel));
            log.info("联盟升级 allianceId={},newLevel={},reputation={}", allianceId, newLevel, updated.getReputation());
        }
        return updated.getReputation();
    }

    // ----------------------- 贡献值/贡献度 (玩家侧) -----------------------

    /**
     * 给玩家发放贡献值, 同时累计贡献度并更新盟内贡献度周榜。
     *
     * @param contribution 贡献值 (流通货币)
     * @param degree       贡献度 (给联盟提供的声誉量, 周榜口径); 0 表示本次不计贡献度
     * @param allianceId   产生贡献时所在联盟 (周榜归属)
     */
    public void grantContribution(long playerId, long contribution, long degree, long allianceId) {
        if (contribution <= 0 && degree <= 0) {
            return;
        }
        alliancePlayerDao.addContribution(playerId, Math.max(contribution, 0), Math.max(degree, 0));
        if (degree > 0 && allianceId > 0) {
            rankService.addPoints(contribRankKey(allianceId), playerId, (int) degree);
        }
    }

    /**
     * 扣减贡献值 (条件 $inc, 余额不足返回 false)。
     */
    public boolean deductContribution(long playerId, long cost) {
        if (cost <= 0) {
            return true;
        }
        return alliancePlayerDao.tryDeductContribution(playerId, cost);
    }

    // ----------------------- 榜单 key -----------------------

    /**
     * 赛季榜 key (自然月)
     */
    public String seasonRankKey() {
        return AllianceConst.RedisKey.RANK_SEASON_PREFIX + TimeHelper.getMonthNumerical();
    }

    public String seasonRankKey(int yyyyMM) {
        return AllianceConst.RedisKey.RANK_SEASON_PREFIX + yyyyMM;
    }

    /**
     * 盟内贡献度周榜 key (ISO 周)
     */
    public String contribRankKey(long allianceId) {
        return contribRankKey(allianceId, currentWeek());
    }

    public String contribRankKey(long allianceId, String week) {
        return AllianceConst.RedisKey.RANK_CONTRIB_PREFIX + allianceId + ":" + week;
    }

    /**
     * 当前 ISO 周标识, 如 2026W24
     */
    public String currentWeek() {
        LocalDate date = LocalDate.now();
        WeekFields wf = WeekFields.ISO;
        return date.get(wf.weekBasedYear()) + "W" + String.format("%02d", date.get(wf.weekOfWeekBasedYear()));
    }

    // ----------------------- 通知 -----------------------

    /**
     * 向联盟全体在线成员广播通用变更通知。
     */
    public void broadcastToAlliance(long allianceId, int notifyType, String param) {
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null || alliance.getMembers() == null || alliance.getMembers().isEmpty()) {
            return;
        }
        NotifyAlliance notify = new NotifyAlliance(Code.SUCCESS);
        notify.type = notifyType;
        notify.allianceId = allianceId;
        notify.param = param;
        socialSender.sendTo(alliance.getMembers().keySet(), notify);
    }

    /**
     * 向单个玩家发送通用变更通知 (被踢/审批结果等)。
     */
    public void notifyPlayer(long playerId, int notifyType, long allianceId, String param) {
        NotifyAlliance notify = new NotifyAlliance(Code.SUCCESS);
        notify.type = notifyType;
        notify.allianceId = allianceId;
        notify.param = param;
        socialSender.sendTo(playerId, notify);
    }
}
