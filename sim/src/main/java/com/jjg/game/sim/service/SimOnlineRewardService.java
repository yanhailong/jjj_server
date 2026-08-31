package com.jjg.game.sim.service;

import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.ResSimClaimOnlineReward;
import com.jjg.game.sim.pb.res.ResSimOnlineReward;
import com.jjg.game.sim.pb.struct.OnlineRewardInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 在线收益：按当前场景产出快速领取，状态归玩家所有，复用玩家串行执行与自动落库。 */
@Service
public class SimOnlineRewardService {
    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimBuildingService buildingService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private SimCasinoService casinoService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private CoreLogger coreLogger;

    public ResSimOnlineReward getInfo(SimPlayerContext ctx) {
        return getInfo(ctx, configCache.getOnlineRewardDiamondCosts(), globalInt(SimConstant.Global.ONLINE_REWARD_AD_CD));
    }

    private ResSimOnlineReward getInfo(SimPlayerContext ctx, List<Item> costs, int adCdMinutes) {
        if (ctx.getCurrentCasino() == null) {
            return new ResSimOnlineReward(Code.NOT_FOUND);
        }
        OnlineRewardInfo info = new OnlineRewardInfo();
        info.adDailyLimit = globalInt(SimConstant.Global.ONLINE_REWARD_AD_LIMIT);
        info.adRewardHours = globalInt(SimConstant.Global.ONLINE_REWARD_AD_HOURS);
        info.diamondDailyLimit = globalInt(SimConstant.Global.ONLINE_REWARD_DIAMOND_LIMIT);
        info.diamondRewardHours = globalInt(SimConstant.Global.ONLINE_REWARD_DIAMOND_HOURS);
        if (info.adDailyLimit < 0 || info.adRewardHours <= 0
                || adCdMinutes < 0
                || info.diamondDailyLimit < 0 || info.diamondRewardHours <= 0
                || costs == null || costs.size() < info.diamondDailyLimit) {
            return new ResSimOnlineReward(Code.SAMPLE_ERROR);
        }
        long now = System.currentTimeMillis();
        SimBaseData base = ctx.getSimBaseData();
        base.resetOnlineRewardDay(Integer.parseInt(TimeHelper.getDate(now, "yyyyMMdd")));
        info.hourlyRewards = buildingService.computeRewardItems(ctx, 60);
        fillClaimState(info, base, costs, now);
        ResSimOnlineReward res = new ResSimOnlineReward(Code.SUCCESS);
        res.info = info;
        return res;
    }

    public ResSimClaimOnlineReward claim(SimPlayerContext ctx, int type) {
        if (type != 0 && type != 1) {
            return new ResSimClaimOnlineReward(Code.PARAM_ERROR);
        }
        if (ctx.getPlayer() == null) {
            return new ResSimClaimOnlineReward(Code.NOT_FOUND);
        }
        List<Item> costs = configCache.getOnlineRewardDiamondCosts();
        int adCdMinutes = globalInt(SimConstant.Global.ONLINE_REWARD_AD_CD);
        ResSimOnlineReward current = getInfo(ctx, costs, adCdMinutes);
        ResSimClaimOnlineReward res = new ResSimClaimOnlineReward(current.code);
        res.info = current.info;
        if (current.code != Code.SUCCESS) {
            return res;
        }
        long now = System.currentTimeMillis();
        SimBaseData base = ctx.getSimBaseData();
        boolean watchAd = type == 0;
        int count = watchAd ? base.getOnlineRewardAdCount() : base.getOnlineRewardDiamondCount();
        int limit = watchAd ? res.info.adDailyLimit : res.info.diamondDailyLimit;
        if (count >= limit) {
            res.code = Code.TODAY_CLIAM_LIMIT;
        } else if (watchAd && now < base.getOnlineRewardAdCdEndTime()) {
            res.code = Code.FORBID;
        }
        if (res.code != Code.SUCCESS) {
            return res;
        }

        int hours = watchAd ? res.info.adRewardHours : res.info.diamondRewardHours;
        Map<BuildingOutputType, Long> rewards = new EnumMap<>(BuildingOutputType.class);
        for (ItemInfo item : res.info.hourlyRewards) {
            rewards.put(BuildingOutputType.fromCode(item.itemId), Math.multiplyExact(item.count, hours));
        }
        rewards.values().removeIf(countValue -> countValue <= 0);
        if (rewards.isEmpty()) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        //经验可能引发升级，必须在可失败的扣费/道具入账成功后交回场景领域发放。
        long exp = rewards.getOrDefault(BuildingOutputType.CASINO_LEVEL_EXP, 0L);
        Map<Integer, Long> items = buildingService.toItemMap(rewards);
        CommonResult<ItemOperationResult> result = watchAd
                ? playerPackService.addItems(ctx.playerId(), items, AddType.SIM_ONLINE_REWARD)
                : playerPackService.useItem(ctx.getPlayer(), res.info.diamondCost.itemId,
                        res.info.diamondCost.count, items, AddType.SIM_ONLINE_REWARD);
        if (result == null || !result.success()) {
            res.code = result == null ? Code.FAIL : result.code;
            return res;
        }

        //先记录领取次数和冷却，再处理升级、统计和通知。
        if (watchAd) {
            base.setOnlineRewardAdCount(count + 1);
            base.setOnlineRewardAdCdEndTime(System.currentTimeMillis()
                    + adCdMinutes * TimeHelper.ONE_MINUTE_OF_MILLIS);
            base.incWatchAdCount();
        } else {
            base.setOnlineRewardDiamondCount(count + 1);
        }
        ctx.setLastSaveTime(0);
        if (exp > 0) {
            long beforeExp = ctx.getCurrentCasino().getExp();
            casinoService.addCasinoExp(ctx, exp);
            coreLogger.addItems(ctx.playerId(), Map.of(SimConstant.Item.CASINO_EXP, beforeExp),
                    Map.of(SimConstant.Item.CASINO_EXP, exp),
                    Map.of(SimConstant.Item.CASINO_EXP, (long) ctx.getCurrentCasino().getExp()),
                    AddType.SIM_ONLINE_REWARD, "在线收益娱乐城经验");
        }
        base.addBusinessIncome(rewards.getOrDefault(BuildingOutputType.GOLD, 0L));
        allianceEventService.onBusinessIncome(ctx.playerId(), items);
        if (watchAd) {
            allianceEventService.onAdWatch(ctx.playerId());
        }
        res.rewards = buildingService.buildRewardInfos(rewards);
        fillClaimState(res.info, base, costs, System.currentTimeMillis());
        return res;
    }

    private int globalInt(int id) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        return cfg == null ? -1 : cfg.getIntValue();
    }

    private void fillClaimState(OnlineRewardInfo info, SimBaseData base, List<Item> costs, long now) {
        info.adUsedCount = base.getOnlineRewardAdCount();
        info.adCdEndTime = base.getOnlineRewardAdCdEndTime() > now ? base.getOnlineRewardAdCdEndTime() : 0;
        info.diamondUsedCount = base.getOnlineRewardDiamondCount();
        if (info.diamondUsedCount < info.diamondDailyLimit) {
            Item cost = costs.get(base.getOnlineRewardDiamondCount());
            info.diamondCost = ItemUtils.buildItemInfo(cost.getId(), cost.getItemCount());
        } else {
            info.diamondCost = null;
        }
    }
}
