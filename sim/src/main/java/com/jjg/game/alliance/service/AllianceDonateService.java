package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.pb.AlliancePbConverter;
import com.jjg.game.alliance.pb.res.ResDonate;
import com.jjg.game.alliance.pb.res.ResDonateInfo;
import com.jjg.game.alliance.pb.struct.AllianceDonateInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.PlayerPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

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
    private AllianceConfigService configService;
    @Autowired
    private AllianceAssetService assetService;
    @Autowired
    private PlayerPackService playerPackService;

    /**
     * 捐献界面信息。
     */
    public ResDonateInfo donateInfo(long playerId) {
        ResDonateInfo res = new ResDonateInfo(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        int donated = playerData.donateCountOf(today);

        res.alliance = AlliancePbConverter.toBrief(alliance, configService);
        res.dailyLimit = AllianceConst.Cfg.DAILY_DONATE_LIMIT;
        res.remainCount = Math.max(0, AllianceConst.Cfg.DAILY_DONATE_LIMIT - donated);
        res.donates = new ArrayList<>();
        for (AllianceConfigService.DonateCfg cfg : configService.donateCfgs()) {
            AllianceDonateInfo info = new AllianceDonateInfo();
            info.donateId = cfg.donateId();
            info.costItemId = cfg.costItemId();
            info.costCount = cfg.costCount();
            info.rewardContribution = cfg.rewardContribution();
            info.rewardReputation = cfg.rewardReputation();
            //当日首捐免费
            info.free = cfg.firstFree() && donated == 0;
            res.donates.add(info);
        }
        return res;
    }

    /**
     * 捐献。
     */
    public ResDonate donate(PlayerController pc, int donateId) {
        ResDonate res = new ResDonate(Code.SUCCESS);
        long playerId = pc.playerId();
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            log.warn("联盟捐献失败,玩家不在联盟 playerId={},donateId={}", playerId, donateId);
            return res;
        }
        AllianceConfigService.DonateCfg cfg = configService.donateCfg(donateId);
        if (cfg == null) {
            res.code = Code.PARAM_ERROR;
            log.warn("联盟捐献失败,捐献配置不存在 playerId={},donateId={}", playerId, donateId);
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        int donated = playerData.donateCountOf(today);
        if (donated >= AllianceConst.Cfg.DAILY_DONATE_LIMIT) {
            res.code = Code.ALLIANCE_DONATE_LIMIT;
            log.warn("联盟捐献失败,今日捐献次数已达上限 playerId={},donated={},limit={}", playerId, donated, AllianceConst.Cfg.DAILY_DONATE_LIMIT);
            return res;
        }

        //扣消耗 (当日首捐可免费)
        int newDonateCount = alliancePlayerDao.reserveDonate(playerId, today, AllianceConst.Cfg.DAILY_DONATE_LIMIT);
        if (newDonateCount <= 0) {
            res.code = Code.ALLIANCE_DONATE_LIMIT;
            log.warn("联盟捐献失败,占用捐献次数失败(并发达上限) playerId={},donateId={},limit={}", playerId, donateId, AllianceConst.Cfg.DAILY_DONATE_LIMIT);
            return res;
        }
        boolean free = cfg.firstFree() && newDonateCount == 1;
        if (!free && cfg.costItemId() > 0 && cfg.costCount() > 0) {
            var deduct = playerPackService.removeItems(pc.getPlayer(),
                    Map.of(cfg.costItemId(), cfg.costCount()), AddType.ALLIANCE_DONATE, "联盟捐献");
            if (!deduct.success()) {
                alliancePlayerDao.rollbackDonate(playerId, today);
                res.code = Code.NOT_ENOUGH_ITEM;
                log.warn("联盟捐献失败,消耗道具不足已回滚 playerId={},donateId={},costItemId={},costCount={}", playerId, donateId, cfg.costItemId(), cfg.costCount());
                return res;
            }
        }

        //计数 + 入账 (贡献值给个人, 声誉给联盟, 贡献度计入周榜)
        assetService.grantContribution(playerId, cfg.rewardContribution(), cfg.rewardReputation(), allianceId);
        long newReputation = assetService.grantReputation(allianceId, cfg.rewardReputation());

        res.rewardContribution = cfg.rewardContribution();
        res.rewardReputation = cfg.rewardReputation();
        res.myContribution = alliancePlayerDao.getOrEmpty(playerId).getContribution();
        res.allianceReputation = Math.max(newReputation, 0);
        res.remainCount = Math.max(0, AllianceConst.Cfg.DAILY_DONATE_LIMIT - newDonateCount);
        log.info("联盟捐献 playerId={},allianceId={},donateId={},free={}", playerId, allianceId, donateId, free);
        return res;
    }
}
