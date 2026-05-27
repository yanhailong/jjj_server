package com.jjg.game.sim.service.tick;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.InteractionAreasTableCfg;
import com.jjg.game.sampledata.bean.VisitorCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.CasinoData;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.NotifyGenerateGuest;
import com.jjg.game.sim.pb.struct.DestinationInfo;
import com.jjg.game.sim.service.SimConfigCacheService;
import com.jjg.game.sim.service.SimRewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 游客生成、解锁
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimGuestService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SimGuestService.class);

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimRewardService rewardService;

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        generateGuestEvent(ctx, now);
    }

    @Override
    public int order() {
        return 10;
    }

    /**
     * 生成游客 (定时器入口)
     */
    public void generateGuestEvent(SimPlayerContext ctx, long now) {
        //新手引导未完成不生成
        if (!ctx.getPlayerGameData().isGuide()) {
            return;
        }
        //当前赌场
        CasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成游客失败，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getPlayerGameData().getCurrentCasinoId());
            return;
        }
        //赌场配置
        CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
        if (casinoCfg == null) {
            log.warn("生成游客失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={}", ctx.playerId(), casino.getStatsId());
            return;
        }
        //计算实际生成间隔(ms)
        long intervalMs = computeVisitIntervalMs(casinoCfg, casino, now);
        if (casino.getLastGenerateTime() != 0 && now - casino.getLastGenerateTime() < intervalMs) {
            return;
        }

        //检查是否有解锁的游客
        if (casino.getGuestMap() == null || casino.getGuestMap().isEmpty()) {
            return;
        }

        //是否曝光
        boolean exposed = casino.getExposureEndTime() > now;

        //加权随机选一个已解锁的游客 (按 BaseWeight / RefreshWeights)
        GuestData guest = pickGuestByWeight(casino, exposed);
        if (guest == null) {
            casino.setLastGenerateTime(now);
            return;
        }
        VisitorCfg visitorCfg = GameDataManager.getVisitorCfg(guest.getId());
        if (visitorCfg == null) {
            casino.setLastGenerateTime(now);
            return;
        }

        //本次交互次数
        Map<Integer, Map<Integer, VisitorStarCfg>> starMap = configCache.getVisitorStarCfgMap();
        int interactionCount = computeInteractionCount(visitorCfg.getBaseDwellTime(), casinoCfg.getProsperity(),
                rewardService.getStarCfg(visitorCfg.getQuality(), guest.getStar(), starMap));
        if (interactionCount <= 0) {
            casino.setLastGenerateTime(now);
            log.info("生成游客但交互次数为 0, 跳过 playerId={},guestId={}", ctx.playerId(), visitorCfg.getId());
            return;
        }

        //规划本次行程的目的地序列
        List<DestinationInfo> destinations = planDestinations(visitorCfg, interactionCount);
        if (destinations.isEmpty()) {
            casino.setLastGenerateTime(now);
            log.warn("生成游客失败，目的地序列为空 playerId={},guestId={}", ctx.playerId(), visitorCfg.getId());
            return;
        }

        //累加经验
        guest.addExp(configCache.getVisitorLevelCfgMap());

        //一次性结算所有交互奖励 (每个目的地一次)
        for (DestinationInfo info : destinations) {
            rewardService.grantReward(guest, info);
        }

        casino.setLastGenerateTime(now);
        ctx.markCasinoDirty();

        //下发通知 (包含完整 destinations, 客户端自行模拟整个流程)
        NotifyGenerateGuest notify = new NotifyGenerateGuest(Code.SUCCESS);
        notify.guests = Collections.singletonList(SimPbConverter.toGuestInfo(guest, destinations));
        ctx.send(notify);

        log.info("生成游客 playerId={},guestId={},exposed={},exp={},level={},destinations={}",
                ctx.playerId(), visitorCfg.getId(), exposed, guest.getExp(), guest.getLevel(), JSON.toJSONString(destinations));
    }

    /**
     * 计算实际来访间隔 (ms)
     */
    private long computeVisitIntervalMs(CasinoStatsSheetCfg casinoCfg, CasinoData casino, long now) {
        long base = (long) casinoCfg.getBaseVisitInterval() * 1000L;
        if (casino.getExposureEndTime() <= now) {
            return base;
        }
        int coefficient = casinoCfg.getVisitIntervalCoefficient();
        if (coefficient <= 0) {
            return base;
        }
        return base * coefficient / SimConstant.Common.EXPOSURE_COEFFICIENT_BASE;
    }

    /**
     * 计算本次来访的交互次数:
     * 1) BaseDwellTime 先按 VisitorStarCfg.Additioncoefficient (百分比) 加成
     * 2) floor(adjustedDwellTime / Prosperity) + 小数部分概率触发 +1
     */
    private int computeInteractionCount(int baseDwellTime, int prosperity, VisitorStarCfg starCfg) {
        if (prosperity <= 0) {
            return 0;
        }

        //星级有时长加成
        if (starCfg != null && starCfg.getAdditioncoefficient() > 0) {
            baseDwellTime = (int) ((long) baseDwellTime * starCfg.getAdditioncoefficient() / 100);
        }

        double ratio = (double) baseDwellTime / (double) prosperity;
        int floor = (int) Math.floor(ratio);
        double frac = ratio - floor;
        if (frac > 0 && RandomUtils.getRandomNumDou() < frac) {
            floor += 1;
        }
        return floor;
    }

    /**
     * 按已解锁游客的 BaseWeight / RefreshWeights 加权随机一个游客
     */
    private GuestData pickGuestByWeight(CasinoData casino, boolean exposed) {
        WeightRandom<GuestData> random = WeightRandom.create();
        for (GuestData g : casino.getGuestMap().values()) {
            VisitorCfg cfg = GameDataManager.getVisitorCfg(g.getId());
            if (cfg == null) {
                continue;
            }
            int weight = exposed ? cfg.getRefreshWeights() : cfg.getBaseWeight();
            if (weight > 0) {
                random.add(g, weight);
            }
        }
        return random.next();
    }

    /**
     * 规划本次行程的目的地序列
     */
    private List<DestinationInfo> planDestinations(VisitorCfg visitorCfg, int interactionCount) {
        List<DestinationInfo> result = new ArrayList<>(interactionCount);

        //随机模式
        List<List<Integer>> interactionWeight = visitorCfg.getInteractionWeight();
        if (interactionWeight != null && !interactionWeight.isEmpty()) {
            for (int i = 0; i < interactionCount; i++) {
                DestinationInfo dest = pickRandomDestination(visitorCfg);
                if (dest != null) {
                    result.add(dest);
                }
            }
            return result;
        }

        //固定模式: 按顺序循环 TargetArea
        List<Integer> targetArea = visitorCfg.getTargetArea();
        if (targetArea != null && !targetArea.isEmpty()) {
            int len = targetArea.size();
            for (int i = 0; i < interactionCount; i++) {
                int buildingId = targetArea.get(i % len);
                DestinationInfo dest = pickBuildingDevice(buildingId);
                if (dest != null) {
                    result.add(dest);
                }
            }
            return result;
        }

        return result;
    }

    /**
     * 在指定建筑随机挑一个交互设备
     */
    private DestinationInfo pickBuildingDevice(int buildingId) {
        InteractionAreasTableCfg cfg = GameDataManager.getInteractionAreasTableCfg(buildingId);
        if (cfg == null || cfg.getDeviceLimit() == null || cfg.getDeviceLimit().isEmpty()) {
            return null;
        }
        int deviceId = cfg.getDeviceLimit().get(RandomUtils.randomInt(cfg.getDeviceLimit().size()));

        DestinationInfo destinationInfo = new DestinationInfo();
        destinationInfo.buildingId = buildingId;
        destinationInfo.deviceId = deviceId;
        return destinationInfo;
    }

    /**
     * 按 InteractionWeight 加权随机一个建筑 + 设备
     */
    private DestinationInfo pickRandomDestination(VisitorCfg visitorCfg) {
        List<List<Integer>> weightList = visitorCfg.getInteractionWeight();
        WeightRandom<Integer> random = WeightRandom.create();
        for (List<Integer> pair : weightList) {
            if (pair == null || pair.size() < 2) {
                continue;
            }
            int weight = pair.get(1);
            if (weight > 0) {
                random.add(pair.get(0), weight);
            }
        }
        Integer buildingId = random.next();
        if (buildingId == null) {
            return null;
        }
        return pickBuildingDevice(buildingId);
    }

    // ---------------------------------------------------------------------
    // 解锁
    // ---------------------------------------------------------------------

    /**
     * 解锁游客
     */
    public int unlockGuest(SimPlayerContext ctx, int guestId) {
        CasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("解锁游客时，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getPlayerGameData().getCurrentCasinoId());
            return Code.NOT_FOUND;
        }
        GuestData guest = casino.findGuestData(guestId);
        if (guest == null) {
            VisitorCfg visitorCfg = GameDataManager.getVisitorCfg(guestId);
            if (visitorCfg == null) {
                log.warn("解锁游客时，未找到对应的配置 guestId={}", guestId);
                return Code.NOT_FOUND;
            }
            guest = new GuestData();
            guest.setId(guestId);
            guest.setLevel(1);
            guest.setStar(1);
            casino.addGuest(guest);
            ctx.markCasinoDirty();
        }

        log.info("解锁游客成功 guestId={}", guestId);
        return Code.SUCCESS;
    }
}
