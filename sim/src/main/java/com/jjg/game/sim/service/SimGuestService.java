package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.NotifyGenerateGuest;
import com.jjg.game.sim.pb.struct.DestinationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        if (!ctx.getSimBaseData().isGuide()) {
            return;
        }
        //当前赌场
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成游客失败，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
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
            casino.setLastGenerateTime(now);
            return;
        }

        //加权随机选一个已解锁的游客
        GuestData guest = pickGuestByWeight(casino);
        if (guest == null) {
            casino.setLastGenerateTime(now);
            return;
        }
        VisitorQuestCfg visitorQuestCfg = GameDataManager.getVisitorQuestCfg(guest.getId());
        if (visitorQuestCfg == null) {
            casino.setLastGenerateTime(now);
            log.warn("生成游客失败，VisitorQuest 配置未找到 playerId={},guestId={}", ctx.playerId(), guest.getId());
            return;
        }

        //本次交互次数 (有奖励 + 无奖励)
        VisitorStarCfg starCfg = rewardService.getStarCfg(guest.getId(), guest.getStar());
        int rewardedCount = computeInteractionCount(visitorQuestCfg.getServiceCapacity(), casinoCfg.getProsperity(), starCfg);
        int unrewardedCount = computeInteractionCount(visitorQuestCfg.getBaseServiceCapacity(), casinoCfg.getProsperity(), starCfg);
        if (rewardedCount + unrewardedCount <= 0) {
            casino.setLastGenerateTime(now);
            log.info("生成游客但交互次数为 0, 跳过 playerId={},guestId={}", ctx.playerId(), visitorQuestCfg.getId());
            return;
        }

        //规划本次行程的目的地序列
        List<DestinationInfo> destinations = planDestinations(guest, visitorQuestCfg, casino, rewardedCount, unrewardedCount);
        if (destinations.isEmpty()) {
            casino.setLastGenerateTime(now);
            log.warn("生成游客失败，目的地序列为空 playerId={},guestId={}", ctx.playerId(), visitorQuestCfg.getId());
            return;
        }

        //累加经验
        guest.addExp(configCache.getVisitorLevelCfgMap());

        casino.setLastGenerateTime(now);
        casino.recordGenerate(now, SimConstant.Common.CAPACITY_WINDOW_MS);

        //下发通知 (包含完整 destinations, 客户端自行模拟整个流程)
        NotifyGenerateGuest notify = new NotifyGenerateGuest(Code.SUCCESS);
        notify.guests = Collections.singletonList(SimPbConverter.toGuestInfo(guest, destinations));
        ctx.send(notify);

        log.info("生成游客 playerId={},guestId={},exp={},level={},rewarded={},unrewarded={},destinations={}",
                ctx.playerId(), visitorQuestCfg.getId(), guest.getExp(), guest.getLevel(), rewardedCount, unrewardedCount, JSON.toJSONString(destinations));
    }

    /**
     * 计算实际来访间隔 (ms)
     * 当赌场满员 (10 分钟内生成人数 >= 赌场容纳上限) 时降低刷新频率:
     * 实际间隔 = 基础间隔 * 当前总人数 / 上限
     */
    private long computeVisitIntervalMs(CasinoStatsSheetCfg casinoCfg, SimCasinoData casino, long now) {
        long base = (long) casinoCfg.getBaseVisitInterval() * 1000L;
        CasinoListCfg listCfg = GameDataManager.getCasinoListCfg(casino.getCasinoId());
        if (listCfg == null || listCfg.getCapacityNum() <= 0) {
            return base;
        }
        int capacity = listCfg.getCapacityNum();
        int recent = casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        if (recent < capacity) {
            return base;
        }
        return base * recent / capacity;
    }

    /**
     * 计算单类交互次数 (有奖励 / 无奖励):
     * 1) 服务能力先按 VisitorStarCfg.Additioncoefficient (百分比) 加成
     * 2) floor(serviceCapacity / Prosperity) + 小数部分概率触发 +1
     */
    private int computeInteractionCount(int serviceCapacity, int prosperity, VisitorStarCfg starCfg) {
        if (serviceCapacity <= 0 || prosperity <= 0) {
            return 0;
        }
        int coefficient = SimConstant.Common.SERVICE_CAPACITY_COEFFICIENT_BASE;
        if (starCfg != null && starCfg.getAdditioncoefficient() > 0) {
            coefficient = starCfg.getAdditioncoefficient();
        }
        double effective = (double) serviceCapacity * coefficient / SimConstant.Common.SERVICE_CAPACITY_COEFFICIENT_BASE;
        double ratio = effective / (double) prosperity;
        int floor = (int) Math.floor(ratio);
        double frac = ratio - floor;
        if (frac > 0 && RandomUtils.getRandomNumDou() < frac) {
            floor += 1;
        }
        return floor;
    }

    /**
     * 按已解锁游客的刷新权重加权随机一个游客
     * 权重 = (cfg.awareness == 0) ? BaseWeight : (cfg.awareness + 1) * BaseWeight
     */
    private GuestData pickGuestByWeight(SimCasinoData casino) {
        WeightRandom<GuestData> random = WeightRandom.create();
        for (GuestData g : casino.getGuestMap().values()) {
            VisitorQuestCfg cfg = GameDataManager.getVisitorQuestCfg(g.getId());
            if (cfg == null) {
                continue;
            }
            int weight = effectiveRefreshWeight(cfg);
            if (weight > 0) {
                random.add(g, weight);
            }
        }
        return random.next();
    }

    /**
     * 计算游客的有效刷新权重 (考虑知名度修正)
     */
    private int effectiveRefreshWeight(VisitorQuestCfg cfg) {
        int base = cfg.getBaseWeight();
        if (cfg.getAwareness() <= 0) {
            return base;
        }
        return (cfg.getAwareness() + 1) * base;
    }

    /**
     * 规划本次行程的目的地序列:
     * - 有奖励交互: 按 InteractionWeight 加权随机一个建筑
     * - 无奖励交互: 按 TargetArea 顺序循环
     * - 两类穿插; 简单实现为先有奖励, 再无奖励
     * - 目标建筑未解锁/未配置则跳过该次交互
     */
    private List<DestinationInfo> planDestinations(GuestData guest, VisitorQuestCfg cfg, SimCasinoData casino, int rewardedCount, int unrewardedCount) {
        List<DestinationInfo> result = new ArrayList<>(rewardedCount + unrewardedCount);

        //有奖励: InteractionWeight (Map<buildingId, weight>)
        Map<Integer, Integer> interactionWeight = cfg.getInteractionWeight();
        if (rewardedCount > 0 && interactionWeight != null && !interactionWeight.isEmpty()) {
            for (int i = 0; i < rewardedCount; i++) {
                DestinationInfo dest = pickRandomDestination(interactionWeight, casino);
                if (dest != null) {
                    rewardService.grantReward(guest, dest);
                    result.add(dest);
                }
            }
        }

        //无奖励: TargetArea 顺序循环
        List<Integer> targetArea = cfg.getTargetArea();
        if (unrewardedCount > 0 && targetArea != null && !targetArea.isEmpty()) {
            int cursor = 0;
            int safety = unrewardedCount * targetArea.size();
            int added = 0;
            while (added < unrewardedCount && safety-- > 0) {
                int buildingId = targetArea.get(cursor % targetArea.size());
                cursor++;
                DestinationInfo dest = pickBuildingDevice(buildingId, casino);
                if (dest != null) {
                    result.add(dest);
                    added++;
                }
            }
        }

        return result;
    }

    /**
     * 按 InteractionWeight 加权随机一个建筑 + 设备 (建筑必须已解锁)
     */
    private DestinationInfo pickRandomDestination(Map<Integer, Integer> weightMap, SimCasinoData casino) {
        WeightRandom<Integer> random = WeightRandom.create();
        for (Map.Entry<Integer, Integer> en : weightMap.entrySet()) {
            if (en.getValue() == null || en.getValue() <= 0) {
                continue;
            }
            if (!isBuildingUnlocked(en.getKey(), casino)) {
                continue;
            }
            random.add(en.getKey(), en.getValue());
        }
        Integer buildingId = random.next();
        if (buildingId == null) {
            return null;
        }
        return pickBuildingDevice(buildingId, casino);
    }

    /**
     * 在指定建筑随机挑一个交互设备 (建筑必须已解锁)
     */
    private DestinationInfo pickBuildingDevice(int buildingId, SimCasinoData casino) {
        if (!isBuildingUnlocked(buildingId, casino)) {
            return null;
        }
        List<Integer> devices = configCache.getBuildingDevices(buildingId);
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        int deviceId = devices.get(RandomUtils.randomInt(devices.size()));

        DestinationInfo destinationInfo = new DestinationInfo();
        destinationInfo.buildingId = buildingId;
        destinationInfo.deviceId = deviceId;
        return destinationInfo;
    }

    /**
     * 建筑是否已解锁 (casino.buildingData 包含该 buildingId)
     */
    private boolean isBuildingUnlocked(int buildingId, SimCasinoData casino) {
        Map<Integer, BuildingData> buildingData = casino.getBuildingData();
        if (buildingData == null || buildingData.isEmpty()) {
            return false;
        }
        return buildingData.containsKey(buildingId);
    }

    // ---------------------------------------------------------------------
    // 解锁
    // ---------------------------------------------------------------------

    /**
     * 解锁游客
     */
    public int unlockGuest(SimPlayerContext ctx, int guestId) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("解锁游客时，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
            return Code.NOT_FOUND;
        }
        GuestData guest = casino.findGuestData(guestId);
        if (guest == null) {
            VisitorQuestCfg cfg = GameDataManager.getVisitorQuestCfg(guestId);
            if (cfg == null) {
                log.warn("解锁游客时，未找到对应的配置 guestId={}", guestId);
                return Code.NOT_FOUND;
            }
            guest = new GuestData();
            guest.setId(guestId);
            guest.setLevel(1);
            guest.setStar(1);
            casino.addGuest(guest);
        }

        log.info("解锁游客成功 guestId={}", guestId);
        return Code.SUCCESS;
    }
}
