package com.jjg.game.alliance.service;

import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.data.DonateCfg;
import com.jjg.game.alliance.pb.res.ResAllianceDonate;
import com.jjg.game.alliance.pb.res.ResAllianceDonateInfo;
import com.jjg.game.alliance.pb.struct.AllianceDonateInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sim.service.SimConfigCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 联盟捐献: 玩家消耗道具 -> 获得贡献值(个人) + 声誉值(联盟)。
 * <p>
 * 每日次数走玩家文档 (同玩家请求串行, 读改写安全), 0 点自然日切 (yyyyMMdd 比对, 不走配置——需求明确);
 * 当日首次可配置为免费。扣道具成功后才入账, 入账走 {@code AllianceAssetService} 统一链路。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceDonateService {
    private static final Logger log = LoggerFactory.getLogger(AllianceDonateService.class);

    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private SimConfigCacheService configService;
    @Autowired
    private AllianceAssetService assetService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private AllianceRedDotService allianceRedDotService;

    /**
     * 捐献界面信息。
     */
    public ResAllianceDonateInfo donateInfo(long playerId) {
        ResAllianceDonateInfo res = new ResAllianceDonateInfo(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();

        res.todayDonatedCount = playerData.donateCountOf(today);

        if (configService.getAllianceDonateCfg() != null) {
            res.donate = new AllianceDonateInfo();
            res.donate.memberDailyLimit = configService.getAllianceDonateCfg().getMemberDailyLimit();
            res.donate.itemId = configService.getAllianceDonateCfg().getItemId();
            res.donate.itemCounts = configService.getAllianceDonateCfg().getCounts();
            res.donate.rewardContribution = configService.getAllianceDonateCfg().getRewardContribution();
            res.donate.rewardReputation = configService.getAllianceDonateCfg().getRewardReputation();
        }
        return res;
    }

    /**
     * 捐献。
     */
    public ResAllianceDonate donate(PlayerController pc) {
        ResAllianceDonate res = new ResAllianceDonate(Code.SUCCESS);
        long playerId = pc.playerId();
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("联盟捐献失败,玩家不在联盟 playerId={}", playerId);
            return res;
        }
        DonateCfg cfg = configService.getAllianceDonateCfg();
        if (cfg == null) {
            res.code = Code.PARAM_ERROR;
            log.warn("联盟捐献失败,捐献配置不存在 playerId={}", playerId);
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        int donated = playerData.donateCountOf(today);
        if (donated >= cfg.getMemberDailyLimit()) {
            res.code = Code.FORBID;
            log.warn("联盟捐献失败,今日捐献次数已达上限 playerId={},donated={},limit={}", playerId, donated, cfg.getMemberDailyLimit());
            return res;
        }

        //扣消耗
        int newDonateCount = alliancePlayerDao.reserveDonate(playerId, today, cfg.getMemberDailyLimit());
        if (newDonateCount <= 0) {
            res.code = Code.FORBID;
            log.warn("联盟捐献失败,占用捐献次数失败(并发达上限) playerId={},limit={}", playerId, cfg.getMemberDailyLimit());
            return res;
        }
        Long count = cfg.getCounts().get(donated);
        if (count != null && count > 0) {
            var deduct = playerPackService.removeItem(pc.getPlayer(), cfg.getItemId(), count, AddType.ALLIANCE_DONATE);
            if (!deduct.success()) {
                alliancePlayerDao.rollbackDonate(playerId, today);
                res.code = Code.NOT_ENOUGH_ITEM;
                log.warn("联盟捐献失败,消耗道具不足已回滚 playerId={},", playerId);
                return res;
            }
        }

        //计数 + 入账 (贡献值给个人, 声誉给联盟, 贡献度计入周榜)
        assetService.grantContribution(playerId, cfg.getRewardContribution(), cfg.getRewardReputation(), allianceId);
        long newReputation = assetService.grantReputation(allianceId, cfg.getRewardReputation());
        allianceEventService.onDonate(playerId, count == null ? 0 : count);

        res.rewardContribution = cfg.getRewardContribution();
        res.rewardReputation = cfg.getRewardReputation();
        res.myContribution = alliancePlayerDao.getOrEmpty(playerId).getContribution();
        res.allianceReputation = Math.max(newReputation, 0);
        res.remainCount = Math.max(0, cfg.getMemberDailyLimit() - newDonateCount);
        allianceRedDotService.clearFreeDonation(playerId);
        log.info("联盟捐献 playerId={},allianceId={}", playerId, allianceId);
        return res;
    }
}
